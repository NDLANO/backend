/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.handlers.searches.suggestion.{DirectGenerator, PhraseSuggestion}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query, SimpleStringQuery}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.requests.searches.suggestion.SuggestionResult
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse}
import SortOrder.{Asc, Desc}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.language.Language
import no.ndla.language.model.Iso639
import no.ndla.network.tapir.NonEmptyString
import no.ndla.search.AggregationBuilder.getAggregationsFromResult
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaSearchException, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{
  ErrorHandling,
  MultiSearchSuggestionDTO,
  MultiSearchSummaryDTO,
  SearchSuggestionDTO,
  SuggestOptionDTO
}
import no.ndla.searchapi.model.domain.Sort.*
import no.ndla.searchapi.model.domain.*
import no.ndla.searchapi.model.search.{SearchPagination, SearchType}

import java.lang.Math.max
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient & IndexService & SearchConverterService & StrictLogging & Props & BaseIndexService &
    ErrorHandling =>

  trait SearchService {
    import props.{DefaultLanguage, ElasticSearchScrollKeepAlive, MaxPageSize}
    val searchIndex: List[String]
    val indexServices: List[BaseIndexService]

    /** Returns hit as summary
      *
      * @param hit
      *   as json string
      * @param language
      *   language as ISO639 code
      * @return
      *   api-model summary of hit
      */
    private def hitToApiModel(hit: SearchHit, language: String, filterInactive: Boolean): Try[MultiSearchSummaryDTO] = {
      val indexName = hit.index.split("_").headOption.traverse(x => props.indexToSearchType(x))
      indexName.flatMap {
        case Some(SearchType.Articles) =>
          Success(searchConverterService.articleHitAsMultiSummary(hit, language, filterInactive))
        case Some(SearchType.Drafts) =>
          Success(searchConverterService.draftHitAsMultiSummary(hit, language, filterInactive))
        case Some(SearchType.LearningPaths) =>
          Success(searchConverterService.learningpathHitAsMultiSummary(hit, language, filterInactive))
        case Some(SearchType.Concepts) =>
          Success(searchConverterService.conceptHitAsMultiSummary(hit, language))
        case Some(SearchType.Grep) =>
          Failure(NdlaSearchException("Got hit from grep index (SearchType.Grep) in `hitToApiModel`. This is a bug."))
        case None =>
          Failure(NdlaSearchException("Index type was bad when determining search result type."))
      }
    }

    def languageQuery(query: NonEmptyString, field: String, boost: Double, language: String): SimpleStringQuery =
      buildSimpleStringQueryForField(query, field, boost, language, fallback = true, searchDecompounded = true)

    def buildSimpleStringQueryForField(
        query: NonEmptyString,
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
        SearchLanguage.languageAnalyzers.foldLeft(
          SimpleStringQuery(query.underlying, quote_field_suffix = Some(".exact"))
        )((acc, cur) => {
          val base = acc.field(s"$field.${cur.languageTag.toString}", boost)
          if (searchDecompounded) base.field(s"$field.${cur.languageTag.toString}.decompounded", 0.1) else base
        })
      } else {
        val base =
          SimpleStringQuery(query.underlying, quote_field_suffix = Some(".exact")).field(s"$field.$language", boost)
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
          ).ignoreUnmapped(true)
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
    ): Try[Seq[MultiSearchSummaryDTO]] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.traverse(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages =>
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

    protected def getSuggestions(response: SearchResponse): Seq[MultiSearchSuggestionDTO] = {
      response.suggestions.map { case (key, value) =>
        MultiSearchSuggestionDTO(name = key, suggestions = getSuggestion(value))
      }.toSeq
    }

    private def getSuggestion(results: Seq[SuggestionResult]): Seq[SearchSuggestionDTO] = {
      results.map(result =>
        SearchSuggestionDTO(
          text = result.text,
          offset = result.offset,
          length = result.length,
          options = result.options.map(mapToSuggestOption)
        )
      )
    }

    private def mapToSuggestOption(optionsMap: Map[String, Any]): SuggestOptionDTO = {
      val text  = optionsMap.getOrElse("text", "")
      val score = optionsMap.getOrElse("score", 1)
      SuggestOptionDTO(
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

    protected def sortField(field: String, order: SortOrder, missingLast: Boolean = true): FieldSort = {
      val sortDefinition = fieldSort(field).sortOrder(order)
      if (missingLast) sortDefinition.missing("_last") else sortDefinition
    }

    protected def defaultSort(default: String, withLanguage: String, order: SortOrder, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage => DefaultLanguage
        case _                   => language
      }

      sortLanguage match {
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
    }

    def getSortDefinition(sort: Sort, language: String): FieldSort = sort match {
      case ByTitleAsc                   => defaultSort("defaultTitle", "title", Asc, language)
      case ByTitleDesc                  => defaultSort("defaultTitle", "title", Desc, language)
      case ByPrimaryRootAsc             => defaultSort("defaultRoot", "primaryRoot", Asc, language)
      case ByPrimaryRootDesc            => defaultSort("defaultRoot", "primaryRoot", Desc, language)
      case ByParentTopicNameAsc         => defaultSort("defaultParentTopicName", "parentTopicName", Asc, language)
      case ByParentTopicNameDesc        => defaultSort("defaultParentTopicName", "parentTopicName", Desc, language)
      case ByResourceTypeAsc            => defaultSort("defaultResourceTypeName", "resourceTypeName", Asc, language)
      case ByResourceTypeDesc           => defaultSort("defaultResourceTypeName", "resourceTypeName", Desc, language)
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
      case ByPublishedAsc               => sortField("published", Asc)
      case ByPublishedDesc              => sortField("published", Desc)
      case ByFavoritedAsc               => sortField("favorited", Asc)
      case ByFavoritedDesc              => sortField("favorited", Desc)
    }

    private val maxResultWindow = props.ElasticSearchIndexMaxResultWindow
    def getStartAtAndNumResults(page: Int, pageSize: Int): Try[SearchPagination] = {
      val safePageSize = max(pageSize.min(MaxPageSize), 0)
      val safePage     = page.max(1)
      val startAt      = (safePage - 1) * safePageSize
      val resultWindow = startAt + safePageSize
      if (resultWindow > maxResultWindow) {
        logger.info(s"Max supported results are $maxResultWindow, user requested $resultWindow")
        Failure(ResultWindowTooLargeException())
      } else Success(SearchPagination(safePage, safePageSize, startAt))
    }
  }
}
