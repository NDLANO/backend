/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse, SuggestionResult}
import com.sksamuel.elastic4s.mappings.FieldDefinition
import com.sksamuel.elastic4s.searches.aggs.Aggregation
import com.sksamuel.elastic4s.searches.queries.{NestedQuery, Query, SimpleStringQuery}
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.searches.suggestion.{DirectGenerator, PhraseSuggestion}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.language.model.Iso639
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.{DefaultLanguage, ElasticSearchScrollKeepAlive, MaxPageSize}
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.{MultiSearchSuggestion, MultiSearchSummary, SearchSuggestion, SuggestOption}
import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.search.SearchType
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException

import java.lang.Math.max
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with IndexService with SearchConverterService with LazyLogging =>

  trait SearchService {
    val searchIndex: List[String]
    val indexServices: List[IndexService[_]]

    /**
      * Returns hit as summary
      *
      * @param hit as json string
      * @param language language as ISO639 code
      * @return api-model summary of hit
      */
    def hitToApiModel(hit: SearchHit, language: String): MultiSearchSummary = {
      val articleType = SearchApiProperties.SearchDocuments(SearchType.Articles)
      val draftType = SearchApiProperties.SearchDocuments(SearchType.Drafts)
      val learningPathType = SearchApiProperties.SearchDocuments(SearchType.LearningPaths)

      val convertFunc = hit.`type` match {
        case `articleType`      => searchConverterService.articleHitAsMultiSummary _
        case `draftType`        => searchConverterService.draftHitAsMultiSummary _
        case `learningPathType` => searchConverterService.learningpathHitAsMultiSummary _
      }

      convertFunc(hit, language)
    }

    def buildSimpleStringQueryForField(
        query: String,
        field: String,
        boost: Int,
        language: String,
        fallback: Boolean,
        searchDecompounded: Boolean
    ): SimpleStringQuery = {
      val searchLanguage = language match {
        case lang if Iso639.get(lang).isSuccess => lang
        case _                                  => Language.AllLanguages
      }

      if (searchLanguage == Language.AllLanguages || fallback) {
        Language.languageAnalyzers.foldLeft(SimpleStringQuery(query, quote_field_suffix = Some(".exact")))(
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
      val idQuery = id.map(q => termQuery(s"$path.id", q))

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
          nestedQuery("embedResourcesAndIds").query(
            boolQuery().must(
              buildTermQueryForEmbed("embedResourcesAndIds", resource, id, language, fallback)
            )
          )
        )
      }
    }

    protected def getHits(response: SearchResponse, language: String): Seq[MultiSearchSummary] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }
            hitToApiModel(result, matchedLanguage)
          })
        case _ => Seq.empty
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
      phraseSuggestion(name = field)
        .on(s"$field.$language.trigram")
        .addDirectGenerator(DirectGenerator(field = s"$field.$language.trigram", suggestMode = Some("always")))
        .size(1)
        .gramSize(3)
        .text(query)
    }

    protected def getSuggestions(response: SearchResponse): Seq[MultiSearchSuggestion] = {
      response.suggestions.map {
        case (key, value) =>
          MultiSearchSuggestion(name = key, suggestions = getSuggestion(value))
      }.toSeq
    }

    protected[search] def buildTermsAggregation(paths: Seq[String]): Seq[Aggregation] = {
      val rootFields = indexServices.flatMap(_.getMapping.fields)

      val aggregationTrees = paths.flatMap(p => buildAggregationTreeFromPath(p, rootFields).toSeq)
      val initialFakeAggregations = aggregationTrees.flatMap(FakeAgg.seqAggsToSubAggs(_).toSeq)

      /** This fancy block basically merges all the [[FakeAgg]]'s that can be merged together */
      val mergedFakeAggregations = initialFakeAggregations.foldLeft(Seq.empty[FakeAgg])((acc, fakeAgg) => {
        val (hasBeenMerged, merged) = acc.foldLeft((false, Seq.empty[FakeAgg]))((acc, toMerge) => {
          val (curHasBeenMerged, aggs) = acc
          fakeAgg.merge(toMerge) match {
            case Some(merged) => true -> (aggs :+ merged)
            case None         => curHasBeenMerged -> (aggs :+ toMerge)
          }
        })
        if (hasBeenMerged) merged else merged :+ fakeAgg
      })

      mergedFakeAggregations.map(_.convertToReal())
    }

    private def buildAggregationTreeFromPath(path: String,
                                             fieldsInIndex: Seq[FieldDefinition]): Option[Seq[FakeAgg]] = {
      @tailrec
      def _buildAggregationRecursive(parts: Seq[String],
                                     fullPath: String,
                                     fieldsInIndex: Seq[FieldDefinition],
                                     remainder: Seq[String],
                                     parentAgg: Seq[FakeAgg]): Option[(Seq[FakeAgg], Seq[String])] = {
        if (parts.isEmpty) {
          None
        } else {
          fieldsInIndex.filter(_.name == parts.mkString(".")) match {
            case Nil =>
              val (newPath, restOfPath) = parts.splitAt(math.max(parts.size - 1, 1))
              _buildAggregationRecursive(newPath, fullPath, fieldsInIndex, restOfPath ++ remainder, parentAgg)
            case fieldsFound =>
              val fieldTypes = fieldsFound.map(_.`type`).distinct
              val pathSoFar = parts.mkString(".")
              val fullPathSoFar = fullPath.split("\\.").reverse.dropWhile(_ != parts.last).reverse.mkString(".")

              val newParent = fieldTypes match {
                case singleType :: Nil if singleType == "nested" =>
                  val n = FakeNestedAgg(pathSoFar, fullPathSoFar)
                  parentAgg :+ n
                case singleType :: Nil if singleType == "keyword" =>
                  val n = FakeTermAgg(pathSoFar).field(fullPath)
                  parentAgg :+ n
                case _ => parentAgg
              }

              if (remainder.isEmpty) {
                Some(newParent -> Seq.empty)
              } else {
                _buildAggregationRecursive(
                  remainder,
                  fullPath,
                  fieldsFound.head.fields,
                  Seq.empty,
                  newParent
                )
              }
          }
        }
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
            val key = bucket("key").asInstanceOf[String]
            val docCount = bucket("doc_count").asInstanceOf[Int]
            Bucket(key, docCount)
          }.toOption
        })
    }

    private def handleBucketResult(resMap: Map[String, Any], field: Seq[String]): Seq[TermAggregation] = {
      Try {
        val sumOtherDocCount = resMap("sum_other_doc_count").asInstanceOf[Int]
        val docCountErrorUpperBound = resMap("doc_count_error_upper_bound").asInstanceOf[Int]
        val buckets = resMap("buckets").asInstanceOf[Seq[Map[String, Any]]]

        TermAggregation(
          field,
          sumOtherDocCount,
          docCountErrorUpperBound,
          buckets = convertBuckets(buckets)
        )
      }.toOption.toSeq
    }

    private def getTermsAggregationResults(
        m: Map[String, Any],
        fields: Seq[String] = Seq.empty,
        foundBuckets: Seq[TermAggregation] = Seq.empty
    ): Seq[TermAggregation] = {
      m.flatMap {
        case (key, map) =>
          val newMap = Try(map.asInstanceOf[Map[String, Any]]).getOrElse {
            logger.error("Map cast failed")
            Map.empty[String, Any]
          }

          if (newMap.contains("buckets") &&
              newMap.contains("sum_other_doc_count") &&
              newMap.contains("doc_count_error_upper_bound")) {
            handleBucketResult(newMap, fields :+ key)
          } else {
            getTermsAggregationResults(newMap, fields :+ key, foundBuckets)
          }

        case _ => Seq.empty[TermAggregation]
      }.toSeq
    }

    def getSuggestion(results: Seq[SuggestionResult]): Seq[SearchSuggestion] = {
      results.map(
        result =>
          SearchSuggestion(text = result.text,
                           offset = result.offset,
                           length = result.length,
                           options = result.options.map(mapToSuggestOption)))
    }

    def mapToSuggestOption(optionsMap: Map[String, Any]): SuggestOption = {
      val text = optionsMap.getOrElse("text", "")
      val score = optionsMap.getOrElse("score", 1)
      SuggestOption(
        text.asInstanceOf[String],
        score.asInstanceOf[Double]
      )
    }

    def scroll(scrollId: String, language: String, fallback: Boolean): Try[SearchResult] = {
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHits(response.result, language)
          val suggestions = getSuggestions(response.result)
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
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage => DefaultLanguage
        case _                   => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          sortLanguage match {
            case Language.AllLanguages => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _ =>
              fieldSort(s"title.$sortLanguage.raw").sortOrder(SortOrder.Asc).missing("_last").unmappedType("long")
          }
        case Sort.ByTitleDesc =>
          sortLanguage match {
            case Language.AllLanguages => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"title.$sortLanguage.raw").sortOrder(SortOrder.Desc).missing("_last").unmappedType("long")
          }
        case Sort.ByDurationAsc     => fieldSort("duration").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByDurationDesc    => fieldSort("duration").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByRelevanceAsc    => fieldSort("_score").sortOrder(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").sortOrder(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").sortOrder(SortOrder.Desc).missing("_last")
      }
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = max(pageSize.min(MaxPageSize), 0)
      val startAt = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    protected def scheduleIndexDocuments(): Unit

    /**
      * Takes care of logging reindexResults, used in subclasses overriding [[scheduleIndexDocuments]]
      *
      * @param indexName Name of index to use for logging
      * @param reindexFuture Reindexing future to handle
      * @param executor Execution context for the future
      */
    protected def handleScheduledIndexResults(indexName: String, reindexFuture: Future[Try[ReindexResult]])(
        implicit executor: ExecutionContext): Unit = {
      reindexFuture.onComplete {
        case Success(Success(reindexResult: ReindexResult)) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} $indexName in ${reindexResult.millisUsed} ms.")
        case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
        case Failure(ex)          => logger.warn(s"Unable to create index '$indexName': " + ex.getMessage, ex)
      }
    }

    protected def errorHandler[U](failure: Throwable): Failure[U] = {
      failure match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              val msg = s"Index ${e.rf.error.index.getOrElse("")} not found. Scheduling a reindex."
              logger.error(msg)
              scheduleIndexDocuments()
              Failure(new IndexNotFoundException(msg))
            case _ =>
              logger.error(e.getMessage)
              Failure(
                new ElasticsearchException(s"Unable to execute search in ${e.rf.error.index.getOrElse("")}",
                                           e.getMessage))
          }
        case t: Throwable => Failure(t)
      }
    }

  }
}
