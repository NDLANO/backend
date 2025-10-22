/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.RequestFailure
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.domain.*
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.service.ConverterService
import no.ndla.language.Language.{AllLanguages, NoLanguage}
import no.ndla.search.{NdlaE4sClient, IndexNotFoundException, NdlaSearchException}

import java.lang.Math.max
import scala.util.{Failure, Success, Try}

trait SearchService[T](using e4sClient: NdlaE4sClient, converterService: ConverterService, props: Props)
    extends StrictLogging {
  val searchIndex: String

  def scroll(scrollId: String, language: String): Try[SearchResult[T]] = e4sClient
    .execute {
      searchScroll(scrollId, props.ElasticSearchScrollKeepAlive)
    }
    .map(response => {
      val hits = getHits(response.result, language)
      SearchResult[T](
        totalCount = response.result.totalHits,
        page = None,
        pageSize = response.result.hits.hits.length,
        language = language,
        results = hits,
        scrollId = response.result.scrollId,
      )
    })

  /** Returns hit as summary
    *
    * @param hit
    *   as json string
    * @param language
    *   language as ISO639 code
    * @return
    *   api-model summary of hit
    */
  def hitToApiModel(hit: String, language: String): T

  def getHits(response: SearchResponse, language: String): Seq[T] = {
    response.totalHits match {
      case count if count > 0 =>
        val resultArray = response.hits.hits.toList

        resultArray.map(result => {
          val matchedLanguage = language match {
            case AllLanguages => converterService.getLanguageFromHit(result).getOrElse(language)
            case _            => language
          }

          hitToApiModel(result.sourceAsString, matchedLanguage)
        })
      case _ => Seq()
    }
  }

  def getSortDefinition(sort: Sort, language: String): FieldSort = {
    val sortLanguage = language match {
      case NoLanguage => props.DefaultLanguage
      case _          => language
    }

    sort match {
      case Sort.ByTitleAsc => language match {
          case AllLanguages => fieldSort("defaultTitle").order(SortOrder.Asc).missing("_last")
          case _            => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.Asc).missing("_last").unmappedType("long")
        }
      case Sort.ByTitleDesc => language match {
          case AllLanguages => fieldSort("defaultTitle").order(SortOrder.Desc).missing("_last")
          case _            => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.Desc).missing("_last").unmappedType("long")
        }
      case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.Asc)
      case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.Desc)
      case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
      case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
      case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.Asc).missing("_last")
      case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.Desc).missing("_last")
    }
  }

  def countDocuments: Long = {
    val response = e4sClient.execute {
      catCount(searchIndex)
    }

    response match {
      case Success(resp) => resp.result.count
      case Failure(_)    => 0
    }
  }

  def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
    val numResults = max(pageSize.min(props.MaxPageSize), 0)
    val startAt    = (
      page - 1
    ).max(0) * numResults

    (startAt, numResults)
  }

  protected def scheduleIndexDocuments(): Unit

  protected def errorHandler[U](failure: Throwable): Failure[U] = {
    failure match {
      case NdlaSearchException(_, Some(RequestFailure(status, _, _, _)), _, _) if status == 404 =>
        logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
        scheduleIndexDocuments()
        Failure(IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex"))
      case e: NdlaSearchException[?] =>
        logger.error(e.getMessage)
        Failure(NdlaSearchException(s"Unable to execute search in $searchIndex", e))
      case t: Throwable => Failure(t)
    }
  }
}
