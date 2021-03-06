/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.auth.Role
import no.ndla.imageapi.model.ResultWindowTooLargeException
import no.ndla.imageapi.model.api.{ErrorHelpers, ImageMetaSummary}
import no.ndla.imageapi.model.domain.{DBImageMetaInformation, SearchResult, SearchSettings, Sort}
import no.ndla.imageapi.model.search.SearchableImage
import no.ndla.language.Language
import no.ndla.language.model.Iso639
import no.ndla.search.Elastic4sClient
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.read
import cats.implicits._

import scala.util.{Failure, Success, Try}

trait ImageSearchService {
  this: Elastic4sClient
    with ImageIndexService
    with SearchService
    with SearchConverterService
    with Role
    with Props
    with ErrorHelpers
    with DBImageMetaInformation =>
  val imageSearchService: ImageSearchService
  class ImageSearchService extends LazyLogging with SearchService[(SearchableImage, MatchedLanguage)] {
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
        result: Try[SearchResult[(SearchableImage, MatchedLanguage)]]
    ): Try[SearchResult[ImageMetaSummary]] =
      for {
        searchResult <- result
        summaries <- searchResult.results.traverse { case (image, language) =>
          searchConverterService.asImageMetaSummary(image, language)
        }
        convertedResult = searchResult.copy(results = summaries)
      } yield convertedResult

    def scrollV2(scrollId: String, language: String): Try[SearchResult[ImageMetaSummary]] = convertToV2(
      scroll(scrollId, language)
    )

    def matchingQuery(settings: SearchSettings): Try[SearchResult[ImageMetaSummary]] = convertToV2(
      matchingQueryV3(settings)
    )

    def matchingQueryV3(settings: SearchSettings): Try[SearchResult[(SearchableImage, MatchedLanguage)]] = {
      val fullSearch = settings.query match {
        case None => boolQuery()
        case Some(query) =>
          val language = settings.language match {
            case Some(lang) if Iso639.get(lang).isSuccess => lang
            case _                                        => "*"
          }

          val queries = Seq(
            simpleStringQuery(query).field(s"titles.$language", 2),
            simpleStringQuery(query).field(s"alttexts.$language", 1),
            simpleStringQuery(query).field(s"caption.$language", 2),
            simpleStringQuery(query).field(s"tags.$language", 2),
            simpleStringQuery(query).field("contributors", 1),
            idsQuery(query)
          )

          val maybeNoteQuery = Option.when(authRole.userHasWriteRole()) {
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
            nestedQuery("imageFiles", rangeQuery("imageFiles.fileSize").gte(size))
          )
        case _ => None
      }

      val (languageFilter, searchLanguage) = settings.language match {
        case Some(lang) if Iso639.get(lang).isSuccess =>
          (Some(existsQuery(s"titles.$lang")), lang)
        case _ => (None, "*")
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
            getHits(response.result, searchLanguage).map(hits => {
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
