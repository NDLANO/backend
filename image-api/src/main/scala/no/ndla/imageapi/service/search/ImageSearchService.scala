/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ResultWindowTooLargeException
import no.ndla.imageapi.model.api.{ErrorHelpers, ImageMetaSummary}
import no.ndla.imageapi.model.domain.{DBImageMetaInformation, SearchResult, SearchSettings, Sort}
import no.ndla.imageapi.model.search.SearchableImage
import no.ndla.common.implicits._
import no.ndla.language.Language
import no.ndla.language.model.Iso639
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.search.Elastic4sClient
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.read

import scala.util.{Failure, Success, Try}

trait ImageSearchService {
  this: Elastic4sClient
    with ImageIndexService
    with SearchService
    with SearchConverterService
    with Props
    with ErrorHelpers
    with DBImageMetaInformation =>
  val imageSearchService: ImageSearchService
  class ImageSearchService extends StrictLogging with SearchService[(SearchableImage, MatchedLanguage)] {
    import props.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
    private val noCopyright          = boolQuery().not(termQuery("license", "copyrighted"))
    override val searchIndex: String = props.SearchIndex
    override val indexService        = imageIndexService

    def hitToApiModel(hit: String, matchedLanguage: String): Try[(SearchableImage, MatchedLanguage)] = {
      implicit val formats: Formats =
        SearchableLanguageFormats.JSonFormats ++ ImageMetaInformation.jsonEncoders
      val searchableImage = Try(read[SearchableImage](hit))
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
    ): Try[SearchResult[ImageMetaSummary]] =
      for {
        searchResult <- result
        summaries <- searchResult.results.traverse { case (image, language) =>
          searchConverterService.asImageMetaSummary(image, language, user)
        }
        convertedResult = searchResult.copy(results = summaries)
      } yield convertedResult

    def scrollV2(scrollId: String, language: String, user: Option[TokenUser]): Try[SearchResult[ImageMetaSummary]] =
      convertToV2(
        scroll(scrollId, language),
        user
      )

    def matchingQuery(settings: SearchSettings, user: Option[TokenUser]): Try[SearchResult[ImageMetaSummary]] =
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
        case None      => Option.unless(settings.includeCopyrighted)(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
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

      val filters        = List(languageFilter, licenseFilter, sizeFilter, modelReleasedFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new ResultWindowTooLargeException(ErrorHelpers.WindowTooLargeError.description))
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
