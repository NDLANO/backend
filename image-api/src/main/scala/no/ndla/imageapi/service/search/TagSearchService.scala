/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ResultWindowTooLargeException
import no.ndla.imageapi.model.api.ErrorHelpers
import no.ndla.imageapi.model.domain.{SearchResult, Sort}
import no.ndla.imageapi.model.search.SearchableTag
import no.ndla.language.model.Iso639
import no.ndla.search.Elastic4sClient
import org.json4s._
import org.json4s.native.Serialization.read

import scala.util.{Failure, Success, Try}

trait TagSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with TagIndexService
    with SearchConverterService
    with Props
    with ErrorHelpers =>
  val tagSearchService: TagSearchService

  class TagSearchService extends StrictLogging with SearchService[String] {
    import props.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive, TagSearchIndex}
    implicit val formats: Formats    = DefaultFormats
    override val searchIndex: String = TagSearchIndex
    override val indexService        = tagIndexService

    override def hitToApiModel(hit: String, language: String): Try[String] = {
      Try(read[SearchableTag](hit)).map(_.tag)
    }

    override def getSortDefinition(sort: Sort, language: String): FieldSort = {
      sort match {
        case Sort.ByRelevanceAsc  => fieldSort("_score").sortOrder(SortOrder.Asc)
        case Sort.ByRelevanceDesc => fieldSort("_score").sortOrder(SortOrder.Desc)
        case Sort.ByTitleAsc      => fieldSort(s"tag.raw").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByTitleDesc     => fieldSort(s"tag.raw").sortOrder(SortOrder.Desc).missing("_last")
        case _                    => fieldSort(s"tag.raw").sortOrder(SortOrder.Desc).missing("_last")
      }
    }

    def all(
        language: String,
        page: Int,
        pageSize: Int,
        sort: Sort
    ): Try[SearchResult[String]] = executeSearch(language, page, pageSize, sort, boolQuery())

    def matchingQuery(
        query: String,
        searchLanguage: String,
        page: Int,
        pageSize: Int,
        sort: Sort
    ): Try[SearchResult[String]] = {

      val fullQuery = boolQuery()
        .must(
          boolQuery().should(
            matchQuery("tag", query).boost(2),
            prefixQuery("tag", query)
          )
        )

      executeSearch(searchLanguage, page, pageSize, sort, fullQuery)
    }

    def executeSearch(
        language: String,
        page: Int,
        pageSize: Int,
        sort: Sort,
        queryBuilder: BoolQuery
    ): Try[SearchResult[String]] = {

      val (languageFilter, searchLanguage) = language match {
        case lang if Iso639.get(lang).isSuccess =>
          (Some(termQuery("language", lang)), lang)
        case _ => (None, "*")
      }

      val filters        = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(Some(page), Some(pageSize))
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new ResultWindowTooLargeException(ImageErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION))
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .trackTotalHits(true)
          .query(filteredSearch)
          .sortBy(getSortDefinition(sort, searchLanguage))

        val searchWithScroll =
          if (startAt != 0) { searchToExecute }
          else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            getHits(response.result, language).map(hits =>
              SearchResult(
                response.result.totalHits,
                Some(page),
                numResults,
                language,
                hits,
                response.result.scrollId
              )
            )
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }
  }
}
