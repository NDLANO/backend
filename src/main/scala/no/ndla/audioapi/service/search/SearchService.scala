/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties.{DefaultPageSize, ElasticSearchScrollKeepAlive, MaxPageSize}
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.{Language, Sort}
import no.ndla.audioapi.model.domain.{NdlaSearchException, SearchResult}
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import scala.util.{Failure, Success, Try}
import cats.implicits._

trait SearchService {
  this: Elastic4sClient with SearchConverterService =>

  trait SearchService[T] extends LazyLogging {
    val searchIndex: String

    def scroll(scrollId: String, language: String): Try[SearchResult[T]] =
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .flatMap(response => {
          getHits(response.result, language).map(hits => {
            SearchResult[T](
              totalCount = response.result.totalHits,
              page = None,
              pageSize = response.result.hits.hits.length,
              language = if (language == "*") Language.AllLanguages else language,
              results = hits,
              scrollId = response.result.scrollId
            )
          })
        })

    def hitToApiModel(hit: String, language: String): Try[T]

    def getHits(response: SearchResponse, language: String): Try[Seq[T]] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.traverse(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
          })
        case _ => Success(Seq.empty)
      }
    }

    protected def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage | Language.AllLanguages => "*"
        case _                                           => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _   => fieldSort(s"titles.$sortLanguage.raw").order(SortOrder.Asc).missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _   => fieldSort(s"titles.$sortLanguage.raw").order(SortOrder.Desc).missing("_last")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.Desc).missing("_last")
      }
    }

    def getSortDefinition(sort: Sort.Value): FieldSort = {
      sort match {
        case Sort.ByTitleAsc        => fieldSort("title.raw").order(SortOrder.Asc).missing("_last")
        case Sort.ByTitleDesc       => fieldSort("title.raw").order(SortOrder.Desc).missing("_last")
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

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) => if (num > 0) num.min(MaxPageSize) else DefaultPageSize
        case None      => DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None     => 0
      }

      (startAt, numResults)
    }

    protected def scheduleIndexDocuments(): Unit

    protected def errorHandler[U](failure: Throwable): Failure[U] = {
      failure match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              Failure(new IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex"))
            case _ =>
              logger.error(e.getMessage)
              Failure(new ElasticsearchException(s"Unable to execute search in $searchIndex", e.getMessage))
          }
        case t: Throwable => Failure(t)
      }
    }
  }
}
