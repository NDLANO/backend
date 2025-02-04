/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.RequestFailure
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.domain.Sort.*
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.language.Language
import no.ndla.language.Language.{AllLanguages, NoLanguage}
import no.ndla.mapping.ISO639
import no.ndla.search.AggregationBuilder.getAggregationsFromResult
import no.ndla.search.{Elastic4sClient, IndexNotFoundException, NdlaSearchException}

import java.lang.Math.max
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient & SearchConverterService & StrictLogging & Props =>

  trait SearchService[T] {
    import props.*

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
            aggregations = getAggregationsFromResult(response.result),
            scrollId = response.result.scrollId
          )
        })

    def hitToApiModel(hit: String, language: String): T

    def buildTermQueryForEmbed(
        path: String,
        resource: List[String],
        id: Option[String],
        language: String,
        fallback: Boolean
    ): List[Query] = {
      val resourceQueries = boolQuery().should(resource.map(q => termQuery(s"$path.resource", q)))
      val idQuery         = id.map(q => termQuery(s"$path.id", q))

      val queries = idQuery.toList :+ resourceQueries
      if (queries.isEmpty || language == Language.AllLanguages || fallback) queries
      else queries :+ termQuery(s"$path.language", language)
    }

    def buildNestedEmbedField(
        resource: List[String],
        id: Option[String],
        language: String,
        fallback: Boolean
    ): Option[NestedQuery] = {
      val emptyInput = (resource.contains("") || resource.isEmpty) && (id.contains("") || id.isEmpty)
      Option.when(!emptyInput)(
        nestedQuery(
          "embedResourcesAndIds",
          boolQuery().must(
            buildTermQueryForEmbed("embedResourcesAndIds", resource, id, language, fallback)
          )
        )
      )
    }

    def getHits(response: SearchResponse, language: String): Seq[T] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.map(result => {
            val matchedLanguage = language match {
              case AllLanguages =>
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
        orFilter(seq, fields*)
      } else { orFilter(seq, s"$fieldName.$language.raw") }
    }

    def getSortDefinition(sort: Sort, language: String): FieldSort = {
      val sortLanguage = language match {
        case NoLanguage => DefaultLanguage
        case _          => language
      }

      def languageSort(default: String, languageField: String, order: SortOrder): FieldSort = sortLanguage match {
        case Language.AllLanguages =>
          fieldSort(default)
            .sortOrder(order)
            .missing("_last")
        case _ =>
          fieldSort(languageField)
            .sortOrder(order)
            .missing("_last")
            .unmappedType("long")
      }

      sort match {
        case ByTitleAsc =>
          languageSort("defaultTitle", s"title.$sortLanguage.lower", SortOrder.Asc)
        case ByTitleDesc =>
          languageSort("defaultTitle", s"title.$sortLanguage.lower", SortOrder.Desc)
        case ByRelevanceAsc =>
          fieldSort("_score").order(SortOrder.Asc)
        case ByRelevanceDesc =>
          fieldSort("_score").order(SortOrder.Desc)
        case ByLastUpdatedAsc =>
          fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
        case ByLastUpdatedDesc =>
          fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
        case ByIdAsc =>
          fieldSort("id").order(SortOrder.Asc).missing("_last")
        case ByIdDesc =>
          fieldSort("id").order(SortOrder.Desc).missing("_last")
        case ByStatusAsc =>
          fieldSort("status.current").sortOrder(SortOrder.Asc).missing("_last")
        case ByStatusDesc =>
          fieldSort("status.current").sortOrder(SortOrder.Desc).missing("_last")
        case BySubjectAsc =>
          languageSort("defaultSortableSubject", s"sortableSubject.$sortLanguage.raw", SortOrder.Asc)
        case BySubjectDesc =>
          languageSort("defaultSortableSubject", s"sortableSubject.$sortLanguage.raw", SortOrder.Desc)
        case ByConceptTypeAsc =>
          languageSort("defaultSortableConceptType", s"sortableConceptType.$sortLanguage.raw", SortOrder.Asc)
        case ByConceptTypeDesc =>
          languageSort("defaultSortableConceptType", s"sortableConceptType.$sortLanguage.raw", SortOrder.Desc)
        case ByResponsibleLastUpdatedAsc =>
          fieldSort("responsible.lastUpdated").sortOrder(SortOrder.Asc).missing("_last")
        case ByResponsibleLastUpdatedDesc =>
          fieldSort("responsible.lastUpdated").sortOrder(SortOrder.Desc).missing("_last")
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
