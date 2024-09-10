/*
 * Part of NDLA draft_api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.RequestFailure
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.{Query, SimpleStringQuery}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.domain.SearchResult
import no.ndla.audioapi.model.Sort
import no.ndla.language.Language
import no.ndla.language.Language.AllLanguages
import no.ndla.language.model.Iso639
import no.ndla.search.{Elastic4sClient, IndexNotFoundException, NdlaSearchException, SearchLanguage}

import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with SearchConverterService with Props =>

  trait SearchService[T] extends StrictLogging {
    import props._
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
              language,
              results = hits,
              scrollId = response.result.scrollId
            )
          })
        })

    protected def languageSpecificSearch(
        searchField: String,
        language: Option[String],
        query: String,
        boost: Double,
        fallback: Boolean
    ): Query = {
      val searchLanguage = language match {
        case Some(lang) if Iso639.get(lang).isSuccess => lang
        case _                                        => Language.AllLanguages
      }

      if (searchLanguage == Language.AllLanguages || fallback) {
        SearchLanguage.languageAnalyzers
          .foldLeft(SimpleStringQuery(query))((acc, cur) => {
            val languageTag = cur.languageTag.toString
            val fieldBoost  = if (languageTag == searchLanguage) boost + 1 else boost
            acc.field(s"$searchField.$languageTag", fieldBoost)
          })
          .field(s"$searchField.*", boost)
      } else {
        simpleStringQuery(query).field(s"$searchField.$searchLanguage", boost)
      }
    }

    def hitToApiModel(hit: String, language: String): Try[T]

    def getHits(response: SearchResponse, language: String): Try[Seq[T]] = {
      response.totalHits match {
        case count if count > 0 =>
          response.hits.hits.toList.traverse(result => {
            val matchedLanguage = language match {
              case AllLanguages => searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _            => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
          })
        case _ => Success(Seq.empty)
      }
    }

    protected def getSortDefinition(sort: Sort, language: String): FieldSort = {
      val sortLanguage = language match {
        case supportedLanguage if Iso639.get(supportedLanguage).isSuccess =>
          supportedLanguage
        case _ => "*"
      }

      sort match {
        case Sort.ByTitleAsc =>
          sortLanguage match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _ => fieldSort(s"titles.$sortLanguage.raw").order(SortOrder.Asc).missing("_last").unmappedType("long")
          }
        case Sort.ByTitleDesc =>
          sortLanguage match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _ => fieldSort(s"titles.$sortLanguage.raw").order(SortOrder.Desc).missing("_last").unmappedType("long")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.Desc).missing("_last")
      }
    }

    def getSortDefinition(sort: Sort): FieldSort = {
      sort match {
        case Sort.ByTitleAsc       => fieldSort("title.raw").order(SortOrder.Asc).missing("_last").unmappedType("long")
        case Sort.ByTitleDesc      => fieldSort("title.raw").order(SortOrder.Desc).missing("_last").unmappedType("long")
        case Sort.ByRelevanceAsc   => fieldSort("_score").order(SortOrder.Asc)
        case Sort.ByRelevanceDesc  => fieldSort("_score").order(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc => fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
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
}
