/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ResultWindowTooLargeException
import no.ndla.imageapi.model.api.{ErrorHandling, ImageMetaSummaryDTO}
import no.ndla.imageapi.model.domain.{SearchResult, SearchSettings, Sort}
import no.ndla.imageapi.model.search.SearchableImage
import no.ndla.common.implicits.*
import no.ndla.language.Language
import no.ndla.language.model.Iso639
import no.ndla.mapping.License
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.search.Elastic4sClient

import scala.util.{Failure, Success, Try}

trait ImageSearchService {
  this: Elastic4sClient
    with ImageIndexService
    with SearchService
    with SearchConverterService
    with Props
    with ErrorHandling =>
  val imageSearchService: ImageSearchService
  class ImageSearchService extends StrictLogging with SearchService[(SearchableImage, MatchedLanguage)] {
    import props.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
    private val noCopyright                      = boolQuery().not(termQuery("license", License.Copyrighted.toString))
    override val searchIndex: String             = props.SearchIndex
    override val indexService: ImageIndexService = imageIndexService

    def hitToApiModel(hit: String, matchedLanguage: String): Try[(SearchableImage, MatchedLanguage)] = {
      val searchableImage = CirceUtil.tryParseAs[SearchableImage](hit)
      searchableImage.map(image => (image, matchedLanguage))
    }

    override def getSortDefinition(sort: Sort, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage | Language.AllLanguages => "*"
        case _                                           => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          sortLanguage match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw").sortOrder(SortOrder.Asc).missing("_last").unmappedType("long")
          }
        case Sort.ByTitleDesc =>
          sortLanguage match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw").sortOrder(SortOrder.Desc).missing("_last").unmappedType("long")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").sortOrder(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").sortOrder(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").sortOrder(SortOrder.Desc).missing("_last")
      }
    }

    private def convertToV2(
        result: Try[SearchResult[(SearchableImage, MatchedLanguage)]],
        user: Option[TokenUser]
    ): Try[SearchResult[ImageMetaSummaryDTO]] =
      for {
        searchResult <- result
        summaries <- searchResult.results.traverse { case (image, language) =>
          searchConverterService.asImageMetaSummary(image, language, user)
        }
        convertedResult = searchResult.copy(results = summaries)
      } yield convertedResult

    def scrollV2(scrollId: String, language: String, user: Option[TokenUser]): Try[SearchResult[ImageMetaSummaryDTO]] =
      convertToV2(
        scroll(scrollId, language),
        user
      )

    def matchingQuery(settings: SearchSettings, user: Option[TokenUser]): Try[SearchResult[ImageMetaSummaryDTO]] =
      convertToV2(
        matchingQueryV3(settings, user),
        user
      )

    def matchingQueryV3(
        settings: SearchSettings,
        user: Option[TokenUser]
    ): Try[SearchResult[(SearchableImage, MatchedLanguage)]] = {
      val fullSearch = settings.query.emptySomeToNone match {
        case None => boolQuery()
        case Some(query) =>
          val language = if (settings.fallback) "*" else settings.language

          val queries = Seq(
            simpleStringQuery(query).field(s"titles.$language", 2),
            simpleStringQuery(query).field(s"alttexts.$language", 1),
            simpleStringQuery(query).field(s"caption.$language", 2),
            simpleStringQuery(query).field(s"tags.$language", 2),
            simpleStringQuery(query).field("contributors", 1),
            idsQuery(query)
          )

          val maybeNoteQuery = Option.when(user.hasPermission(IMAGE_API_WRITE)) {
            simpleStringQuery(query).field("editorNotes", 1)
          }

          val flattenedQueries = Seq(maybeNoteQuery, queries).flatten
          boolQuery().must(boolQuery().should(flattenedQueries))
      }
      executeSearch(fullSearch, settings)
    }

    def executeSearch(
        queryBuilder: BoolQuery,
        settings: SearchSettings
    ): Try[SearchResult[(SearchableImage, MatchedLanguage)]] = {

      val licenseFilter = settings.license match {
        case Some("all") => None
        case Some(lic)   => Some(termQuery("license", lic))
        case None        => Some(noCopyright)
      }

      val sizeFilter = settings.minimumSize match {
        case Some(size) =>
          Some(
            nestedQuery("imageFiles", rangeQuery("imageFiles.fileSize").gte(size.toLong))
          )
        case _ => None
      }

      val (languageFilter, searchLanguage) =
        if (Iso639.get(settings.language).isSuccess) {
          if (settings.fallback)
            (None, "*")
          else
            (Some(existsQuery(s"titles.${settings.language}")), settings.language)
        } else {
          (None, "*")
        }

      val modelReleasedFilter = Option.when(settings.modelReleased.nonEmpty)(
        boolQuery().should(settings.modelReleased.map(mrs => termQuery("modelReleased", mrs.toString)))
      )

      val podcastFilter = Option.when(settings.podcastFriendly.nonEmpty)(
        boolQuery().should(settings.podcastFriendly.map(pf => termQuery("podcastFriendly", pf.toString)))
      )

      val userFilter = settings.userFilter match {
        case Nil          => None
        case nonEmptyList => Some(termsQuery("users", nonEmptyList))
      }

      val filters = List(languageFilter, licenseFilter, sizeFilter, modelReleasedFilter, podcastFilter, userFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new ResultWindowTooLargeException(ImageErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION))
      } else {
        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .trackTotalHits(true)
            .from(startAt)
            .highlighting(highlight("*"))
            .query(filteredSearch)
            .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            getHits(response.result, settings.language).map(hits => {
              SearchResult(
                response.result.totalHits,
                Some(settings.page.getOrElse(1)),
                numResults,
                searchLanguage,
                hits,
                response.result.scrollId
              )
            })
          case Failure(ex) => errorHandler(ex)
        }
      }
    }
  }
}
