/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.RequestFailure
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.NestedQuery
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.requests.searches.term.TermQuery
import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.ConceptApiProperties.{DefaultLanguage, ElasticSearchScrollKeepAlive, MaxPageSize}
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.language.Language.{AllLanguages, NoLanguage}
import no.ndla.mapping.ISO639
import no.ndla.search.{Elastic4sClient, IndexNotFoundException, NdlaSearchException}

import java.lang.Math.max
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with SearchConverterService with LazyLogging =>

  trait SearchService[T] {
    val searchIndex: String

    def scroll(scrollId: String, language: String): Try[SearchResult[T]] =
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHits(response.result, language)

          SearchResult[T](
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = if (language == "*") AllLanguages else language,
            results = hits,
            scrollId = response.result.scrollId
          )
        })

    def hitToApiModel(hit: String, language: String): T

    def buildTermQueryForEmbed(
        path: String,
        resource: Option[String],
        id: Option[String],
        language: String,
        fallback: Boolean
    ): List[TermQuery] = {
      val queries = (resource, id) match {
        case (Some("") | None, Some("") | None) => List.empty
        case (Some(q), Some("") | None)         => List(termQuery(s"$path.resource", q))
        case (Some("") | None, Some(q))         => List(termQuery(s"$path.id", q))
        case (Some(q1), Some(q2))               => List(termQuery(s"$path.resource", q1), termQuery(s"$path.id", q2))
      }
      if (queries.isEmpty) return queries
      if (language == AllLanguages || fallback) queries
      else queries :+ termQuery(s"$path.language", language)
    }

    def buildNestedEmbedField(
        resource: Option[String],
        id: Option[String],
        language: String,
        fallback: Boolean
    ): Option[NestedQuery] = {
      if ((resource == Some("") || resource.isEmpty) && (id == Some("") || id.isEmpty)) {
        return None
      }
      if (language == AllLanguages || fallback) {
        Some(
          nestedQuery(
            "embedResourcesAndIds",
            boolQuery().must(
              buildTermQueryForEmbed("embedResourcesAndIds", resource, id, language, fallback)
            )
          )
        )
      } else {
        Some(
          nestedQuery(
            "embedResourcesAndIds",
            boolQuery().must(
              buildTermQueryForEmbed("embedResourcesAndIds", resource, id, language, fallback)
            )
          )
        )
      }
    }

    def getHits(response: SearchResponse, language: String): Seq[T] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.map(result => {
            val matchedLanguage = language match {
              case AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    protected def orFilter(seq: Iterable[Any], fieldNames: String*): Option[BoolQuery] =
      if (seq.isEmpty) None
      else
        Some(
          boolQuery().should(
            fieldNames.flatMap(fieldName => seq.map(s => termQuery(fieldName, s)))
          )
        )

    protected def languageOrFilter(
        seq: Iterable[Any],
        fieldName: String,
        language: String,
        fallback: Boolean
    ): Option[BoolQuery] = {
      if (language == AllLanguages || language == "*" || fallback) {
        val fields = ISO639.languagePriority.map(l => s"$fieldName.$l.raw")
        orFilter(seq, fields: _*)
      } else { orFilter(seq, s"$fieldName.$language.raw") }
    }

    def getSortDefinition(sort: Sort, language: String): FieldSort = {
      val sortLanguage = language match {
        case NoLanguage => DefaultLanguage
        case _          => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" | AllLanguages => fieldSort("defaultTitle").order(SortOrder.Asc).missing("_last")
            case _ => fieldSort(s"title.$sortLanguage.lower").order(SortOrder.Asc).missing("_last").unmappedType("long")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" | AllLanguages => fieldSort("defaultTitle").order(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"title.$sortLanguage.lower").order(SortOrder.Desc).missing("_last").unmappedType("long")
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
        case Sort.ByTitleAsc        => fieldSort("title.lower").order(SortOrder.Asc).missing("_last")
        case Sort.ByTitleDesc       => fieldSort("title.lower").order(SortOrder.Desc).missing("_last")
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
      val numResults = max(pageSize.min(MaxPageSize), 0)
      val startAt    = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    protected def scheduleIndexDocuments(): Unit

    protected def errorHandler[U](failure: Throwable): Failure[U] = {
      failure match {
        case NdlaSearchException(_, Some(RequestFailure(status, _, _, _)), _) if status == 404 =>
          logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
          scheduleIndexDocuments()
          Failure(IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex"))
        case e: NdlaSearchException =>
          logger.error(e.getMessage)
          Failure(NdlaSearchException(s"Unable to execute search in $searchIndex", e))
        case t: Throwable => Failure(t)
      }
    }
  }
}
