/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import cats.data.NonEmptySeq
import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.{ElasticField, NestedField, ObjectField, TextField}
import com.sksamuel.elastic4s.handlers.searches.suggestion.{DirectGenerator, PhraseSuggestion}
import com.sksamuel.elastic4s.requests.searches.aggs.Aggregation
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query, SimpleStringQuery}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.requests.searches.suggestion.SuggestionResult
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse}
import SortOrder.{Asc, Desc}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.language.Language
import no.ndla.language.model.Iso639
import no.ndla.search.{Elastic4sClient, IndexNotFoundException, NdlaSearchException, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{MultiSearchSuggestion, MultiSearchSummary, SearchSuggestion, SuggestOption}
import no.ndla.searchapi.model.domain.Sort._
import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.search.SearchType

import java.lang.Math.max
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with IndexService with SearchConverterService with StrictLogging with Props =>

  trait SearchService {
    import props.{DefaultLanguage, ElasticSearchScrollKeepAlive, MaxPageSize}
    val searchIndex: List[String]
    val indexServices: List[IndexService[_]]

    /** Returns hit as summary
      *
      * @param hit
      *   as json string
      * @param language
      *   language as ISO639 code
      * @return
      *   api-model summary of hit
      */
    private def hitToApiModel(hit: SearchHit, language: String, filterInactive: Boolean) = {
      val articleType      = props.SearchIndexes(SearchType.Articles)
      val draftType        = props.SearchIndexes(SearchType.Drafts)
      val learningPathType = props.SearchIndexes(SearchType.LearningPaths)

      hit.index.split("_").headOption match {
        case Some(`articleType`) =>
          Success(searchConverterService.articleHitAsMultiSummary(hit, language, filterInactive))
        case Some(`draftType`) => Success(searchConverterService.draftHitAsMultiSummary(hit, language, filterInactive))
        case Some(`learningPathType`) =>
          Success(searchConverterService.learningpathHitAsMultiSummary(hit, language, filterInactive))
        case _ => Failure(NdlaSearchException("Index type was bad when determining search result type."))
      }
    }

    def buildSimpleStringQueryForField(
        query: String,
        field: String,
        boost: Double,
        language: String,
        fallback: Boolean,
        searchDecompounded: Boolean
    ): SimpleStringQuery = {
      val searchLanguage = language match {
        case lang if Iso639.get(lang).isSuccess => lang
        case _                                  => Language.AllLanguages
      }

      if (searchLanguage == Language.AllLanguages || fallback) {
        SearchLanguage.languageAnalyzers.foldLeft(SimpleStringQuery(query, quote_field_suffix = Some(".exact")))(
          (acc, cur) => {
            val base = acc.field(s"$field.${cur.languageTag.toString}", boost)
            if (searchDecompounded) base.field(s"$field.${cur.languageTag.toString}.decompounded", 0.1) else base
          }
        )
      } else {
        val base = SimpleStringQuery(query, quote_field_suffix = Some(".exact")).field(s"$field.$language", boost)
        if (searchDecompounded) base.field(s"$field.$language.decompounded", 0.1) else base
      }
    }

    private def buildTermQueryForEmbed(
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

    protected def buildNestedEmbedField(
        resource: List[String],
        id: Option[String],
        language: String,
        fallback: Boolean
    ): Option[NestedQuery] = {
      if (resource.isEmpty && id.isEmpty) {
        None
      } else {
        Some(
          nestedQuery(
            "embedResourcesAndIds",
            boolQuery().must(buildTermQueryForEmbed("embedResourcesAndIds", resource, id, language, fallback))
          )
        )
      }
    }

    protected def supportedLanguagesFilter(supportedLanguages: List[String]): Option[BoolQuery] = {
      Option.when(supportedLanguages.nonEmpty) {
        boolQuery().should(supportedLanguages.map(l => termQuery("supportedLanguages", l)))
      }
    }

    protected def getHits(
        response: SearchResponse,
        language: String,
        filterInactive: Boolean
    ): Try[Seq[MultiSearchSummary]] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.traverse(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }
            hitToApiModel(result, matchedLanguage, filterInactive)
          })
        case _ => Success(Seq.empty)
      }
    }

    protected def suggestions(query: Option[String], language: String, fallback: Boolean): Seq[PhraseSuggestion] = {
      query
        .map(q => {
          val searchLanguage =
            if (language == Language.AllLanguages || fallback) DefaultLanguage else language
          Seq(
            suggestion(q, "title", searchLanguage),
            suggestion(q, "content", searchLanguage)
          )
        })
        .getOrElse(Seq.empty)
    }

    private def suggestion(query: String, field: String, language: String): PhraseSuggestion = {
      phraseSuggestion(name = field, field = s"$field.$language.trigram")
        .addDirectGenerator(DirectGenerator(field = s"$field.$language.trigram", suggestMode = Some("always")))
        .size(1)
        .gramSize(3)
        .text(query)
    }

    protected def getSuggestions(response: SearchResponse): Seq[MultiSearchSuggestion] = {
      response.suggestions.map { case (key, value) =>
        MultiSearchSuggestion(name = key, suggestions = getSuggestion(value))
      }.toSeq
    }

    protected[search] def buildTermsAggregation(paths: Seq[String]): Seq[Aggregation] = {
      val indexRootFields: Seq[ElasticField] = indexServices.flatMap(_.getMapping.properties)
      val aggregationTrees        = paths.flatMap(p => buildAggregationTreeFromPath(p, indexRootFields).toSeq)
      val initialFakeAggregations = aggregationTrees.flatMap(FakeAgg.seqAggsToSubAggs(_).toSeq)
      val mergedFakeAggregations  = mergeAllFakeAggregations(initialFakeAggregations)
      mergedFakeAggregations.map(_.convertToReal())
    }

    /** This method merges all the [[FakeAgg]]'s that can be merged together */
    private def mergeAllFakeAggregations(initialFakeAggregations: Seq[FakeAgg]): Seq[FakeAgg] =
      initialFakeAggregations.foldLeft(Seq.empty[FakeAgg])((acc, fakeAgg) => {
        val (hasBeenMerged, merged) = acc.foldLeft((false, Seq.empty[FakeAgg]))((acc, toMerge) => {
          val (curHasBeenMerged, aggs) = acc
          fakeAgg.merge(toMerge) match {
            case Some(merged) => true             -> (aggs :+ merged)
            case None         => curHasBeenMerged -> (aggs :+ toMerge)
          }
        })
        if (hasBeenMerged) merged else merged :+ fakeAgg
      })

    private def buildAggregationTreeFromPath(path: String, fieldsInIndex: Seq[ElasticField]): Option[Seq[FakeAgg]] = {
      @tailrec
      def _buildAggregationRecursive(
          parts: Seq[String],
          fullPath: String,
          fieldsInIndex: Seq[ElasticField],
          remainder: Seq[String],
          parentAgg: Seq[FakeAgg]
      ): Option[(Seq[FakeAgg], Seq[String])] = if (parts.isEmpty) { None }
      else {
        val matchingIndexFields: Seq[ElasticField] = fieldsInIndex.filter(_.name == parts.mkString("."))
        NonEmptySeq.fromSeq(matchingIndexFields) match {
          case None =>
            val (newPath, restOfPath) = parts.splitAt(math.max(parts.size - 1, 1))
            if (parts == newPath) { None }
            else { _buildAggregationRecursive(newPath, fullPath, fieldsInIndex, restOfPath ++ remainder, parentAgg) }
          case Some(fieldsFound) =>
            val fieldTypes    = fieldsFound.map(_.`type`).distinct
            val pathSoFar     = parts.mkString(".")
            val fullPathSoFar = fullPath.split("\\.").reverse.dropWhile(_ != parts.last).reverse.mkString(".")
            val newParent     = newParentAggregation(fullPath, parentAgg, fieldTypes.toList, pathSoFar, fullPathSoFar)

            if (remainder.isEmpty) { Some(newParent -> Seq.empty) }
            else { _buildAggregationRecursive(remainder, fullPath, subfieldsOf(fieldsFound), Seq.empty, newParent) }
        }
      }

      def newParentAggregation(
          fullPath: String,
          parentAgg: Seq[FakeAgg],
          fieldTypes: Seq[String],
          pathSoFar: String,
          fullPathSoFar: String
      ): Seq[FakeAgg] = fieldTypes match {
        case singleType :: Nil if singleType == "nested" =>
          val n = FakeNestedAgg(pathSoFar, fullPathSoFar)
          parentAgg :+ n
        case singleType :: Nil if singleType == "keyword" =>
          val n = FakeTermAgg(pathSoFar).field(fullPath)
          parentAgg :+ n
        case _ => parentAgg
      }

      def subfieldsOf(fieldsFound: NonEmptySeq[ElasticField]): Seq[ElasticField] = fieldsFound.head match {
        case nestedField: NestedField => nestedField.properties
        case objectField: ObjectField => objectField.properties
        case textField: TextField     => textField.fields
        case _                        => Seq.empty
      }

      _buildAggregationRecursive(path.split("\\.").toSeq, path, fieldsInIndex, Seq.empty, Seq.empty).map(_._1)
    }

    def getAggregationsFromResult(response: SearchResponse): Seq[TermAggregation] = {
      getTermsAggregationResults(response.aggs.data)
    }

    private def convertBuckets(buckets: Seq[Map[String, Any]]): Seq[Bucket] = {
      buckets
        .flatMap(bucket => {
          Try {
            val key      = bucket("key").asInstanceOf[String]
            val docCount = bucket("doc_count").asInstanceOf[Int]
            Bucket(key, docCount)
          }.toOption
        })
    }

    private def handleBucketResult(resMap: Map[String, Any], field: Seq[String]): Seq[TermAggregation] = {
      Try {
        val sumOtherDocCount        = resMap("sum_other_doc_count").asInstanceOf[Int]
        val docCountErrorUpperBound = resMap("doc_count_error_upper_bound").asInstanceOf[Int]
        val buckets                 = resMap("buckets").asInstanceOf[Seq[Map[String, Any]]]

        TermAggregation(
          field,
          sumOtherDocCount,
          docCountErrorUpperBound,
          buckets = convertBuckets(buckets)
        )
      }.toOption.toSeq
    }

    private def getTermsAggregationResults(
        aggregationMap: Map[String, Any],
        fields: Seq[String] = Seq.empty,
        foundBuckets: Seq[TermAggregation] = Seq.empty
    ): Seq[TermAggregation] = aggregationMap.toSeq.flatMap { case (key, map) =>
      val newMap = Try(map.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty[String, Any])

      val hasBucketAggregationKeys =
        newMap.contains("buckets") &&
          newMap.contains("sum_other_doc_count") &&
          newMap.contains("doc_count_error_upper_bound")

      if (hasBucketAggregationKeys) {
        handleBucketResult(newMap, fields :+ key)
      } else {
        getTermsAggregationResults(newMap, fields :+ key, foundBuckets)
      }
    }

    def getSuggestion(results: Seq[SuggestionResult]): Seq[SearchSuggestion] = {
      results.map(result =>
        SearchSuggestion(
          text = result.text,
          offset = result.offset,
          length = result.length,
          options = result.options.map(mapToSuggestOption)
        )
      )
    }

    def mapToSuggestOption(optionsMap: Map[String, Any]): SuggestOption = {
      val text  = optionsMap.getOrElse("text", "")
      val score = optionsMap.getOrElse("score", 1)
      SuggestOption(
        text.asInstanceOf[String],
        score.asInstanceOf[Double]
      )
    }

    def scroll(scrollId: String, language: String): Try[SearchResult] = {
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .flatMap(response => {
          getHits(response.result, language, filterInactive = false).map(hits => {
            val suggestions  = getSuggestions(response.result)
            val aggregations = getAggregationsFromResult(response.result)
            SearchResult(
              totalCount = response.result.totalHits,
              page = None,
              pageSize = response.result.hits.hits.length,
              language = language,
              results = hits,
              suggestions = suggestions,
              aggregations = aggregations,
              scrollId = response.result.scrollId
            )
          })
        })
    }

    private def sortField(field: String, order: SortOrder, missingLast: Boolean = true): FieldSort = {
      val sortDefinition = fieldSort(field).sortOrder(order)
      if (missingLast) sortDefinition.missing("_last") else sortDefinition
    }

    def getSortDefinition(sort: Sort, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage => DefaultLanguage
        case _                   => language
      }

      def defaultSort(default: String, withLanguage: String, order: SortOrder): FieldSort = sortLanguage match {
        case Language.AllLanguages =>
          fieldSort(default)
            .sortOrder(order)
            .missing("_last")
        case _ =>
          fieldSort(s"$withLanguage.$sortLanguage.raw")
            .sortOrder(order)
            .missing("_last")
            .unmappedType("long")
      }

      sort match {
        case ByTitleAsc                   => defaultSort("defaultTitle", "title", Asc)
        case ByTitleDesc                  => defaultSort("defaultTitle", "title", Desc)
        case ByPrimaryRootAsc             => defaultSort("defaultRoot", "primaryRoot", Asc)
        case ByPrimaryRootDesc            => defaultSort("defaultRoot", "primaryRoot", Desc)
        case ByParentTopicNameAsc         => defaultSort("defaultParentTopicName", "parentTopicName", Asc)
        case ByParentTopicNameDesc        => defaultSort("defaultParentTopicName", "parentTopicName", Desc)
        case ByResourceTypeAsc            => defaultSort("defaultResourceTypeName", "resourceTypeName", Asc)
        case ByResourceTypeDesc           => defaultSort("defaultResourceTypeName", "resourceTypeName", Desc)
        case ByDurationAsc                => sortField("duration", Asc)
        case ByDurationDesc               => sortField("duration", Desc)
        case ByStatusAsc                  => sortField("draftStatus.current", Asc)
        case ByStatusDesc                 => sortField("draftStatus.current", Desc)
        case ByRelevanceAsc               => sortField("_score", Asc, missingLast = false)
        case ByRelevanceDesc              => sortField("_score", Desc, missingLast = false)
        case ByLastUpdatedAsc             => sortField("lastUpdated", Asc)
        case ByLastUpdatedDesc            => sortField("lastUpdated", Desc)
        case ByIdAsc                      => sortField("id", Asc)
        case ByIdDesc                     => sortField("id", Desc)
        case ByRevisionDateAsc            => sortField("nextRevision.revisionDate", Asc)
        case ByRevisionDateDesc           => sortField("nextRevision.revisionDate", Desc)
        case ByResponsibleLastUpdatedAsc  => sortField("responsible.lastUpdated", Asc)
        case ByResponsibleLastUpdatedDesc => sortField("responsible.lastUpdated", Desc)
        case ByPrioritizedAsc             => sortField("prioritized", Asc)
        case ByPrioritizedDesc            => sortField("prioritized", Desc)
      }
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = max(pageSize.min(MaxPageSize), 0)
      val startAt    = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    protected def scheduleIndexDocuments(): Unit

    /** Takes care of logging reindexResults, used in subclasses overriding [[scheduleIndexDocuments]]
      *
      * @param indexName
      *   Name of index to use for logging
      * @param reindexFuture
      *   Reindexing future to handle
      * @param executor
      *   Execution context for the future
      */
    protected def handleScheduledIndexResults(indexName: String, reindexFuture: Future[Try[ReindexResult]])(implicit
        executor: ExecutionContext
    ): Unit = {
      reindexFuture.onComplete {
        case Success(Success(reindexResult: ReindexResult)) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} $indexName in ${reindexResult.millisUsed} ms."
          )
        case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
        case Failure(ex)          => logger.warn(s"Unable to create index '$indexName': " + ex.getMessage, ex)
      }
    }

    protected def errorHandler[U](failure: Throwable): Failure[U] = {
      failure match {
        case e: NdlaSearchException =>
          e.rf.map(_.status).getOrElse(0) match {
            case notFound: Int if notFound == 404 =>
              val msg = s"Index ${e.rf.flatMap(_.error.index).getOrElse("")} not found. Scheduling a reindex."
              logger.error(msg)
              scheduleIndexDocuments()
              Failure(IndexNotFoundException(msg))
            case _ =>
              logger.error(e.getMessage)
              Failure(
                NdlaSearchException(s"Unable to execute search in ${e.rf.flatMap(_.error.index).getOrElse("")}", e)
              )
          }
        case t: Throwable => Failure(t)
      }
    }

  }
}
