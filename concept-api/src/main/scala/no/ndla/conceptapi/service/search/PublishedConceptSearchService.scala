/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.SimpleQueryStringFlag
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.{ErrorHandling, OperationNotAllowedException, SubjectTagsDTO}
import no.ndla.conceptapi.model.domain.SearchResult
import no.ndla.conceptapi.model.search.{SearchSettings, SearchSettingsHelper}
import no.ndla.conceptapi.service.ConverterService
import no.ndla.language.Language
import no.ndla.language.Language.AllLanguages
import no.ndla.search.AggregationBuilder.{buildTermsAggregation, getAggregationsFromResult}
import no.ndla.search.Elastic4sClient

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait PublishedConceptSearchService {
  this: Elastic4sClient & SearchService & PublishedConceptIndexService & ConverterService & SearchConverterService &
    Props & ErrorHandling & SearchSettingsHelper =>
  val publishedConceptSearchService: PublishedConceptSearchService

  class PublishedConceptSearchService extends StrictLogging with SearchService[api.ConceptSummaryDTO] {
    import props.*
    override val searchIndex: String = PublishedConceptSearchIndex

    override def hitToApiModel(hitString: String, language: String): api.ConceptSummaryDTO =
      searchConverterService.hitAsConceptSummary(hitString, language)

    def getTagsWithSubjects(
        subjectIds: List[String],
        language: String,
        fallback: Boolean
    ): Try[List[api.SubjectTagsDTO]] = {
      if (subjectIds.size <= 0) {
        Failure(OperationNotAllowedException("Will not generate list of subject tags with no specified subjectIds"))
      } else {
        implicit val ec: ExecutionContextExecutor =
          ExecutionContext.fromExecutor(Executors.newFixedThreadPool(subjectIds.size))
        val searches = subjectIds.traverse(subjectId => searchSubjectIdTags(subjectId, language, fallback))
        Await.result(searches, 1 minute).sequence.map(_.flatten)
      }
    }

    private def searchSubjectIdTags(subjectId: String, language: String, fallback: Boolean)(implicit
        executor: ExecutionContext
    ): Future[Try[List[SubjectTagsDTO]]] =
      Future {
        val settings = SearchSettings.empty.copy(
          subjects = Set(subjectId),
          searchLanguage = language,
          fallback = fallback,
          shouldScroll = true
        )

        searchUntilNoMoreResults(settings).map(searchResults => {
          val tagsInSubject = for {
            searchResult <- searchResults
            searchHits   <- searchResult.results
            matchedTags  <- searchHits.tags.toSeq
          } yield matchedTags

          searchConverterService
            .groupSubjectTagsByLanguage(subjectId, tagsInSubject)
            .filter(tags => tags.language == language || language == Language.AllLanguages || fallback)
        })
      }

    @tailrec
    private def searchUntilNoMoreResults(
        searchSettings: SearchSettings,
        prevResults: List[SearchResult[api.ConceptSummaryDTO]] = List.empty
    ): Try[List[SearchResult[api.ConceptSummaryDTO]]] = {
      val page = prevResults.lastOption.flatMap(_.page).getOrElse(0) + 1

      val result = prevResults.lastOption.flatMap(_.scrollId) match {
        case Some(scrollId) => this.scroll(scrollId, searchSettings.searchLanguage)
        case None           => this.all(searchSettings.copy(page = page))
      }

      result match {
        case Failure(ex)                                                        => Failure(ex)
        case Success(value) if value.results.size <= 0 || value.totalCount == 0 => Success(prevResults)
        case Success(value) => searchUntilNoMoreResults(searchSettings, prevResults :+ value)
      }
    }

    def all(settings: SearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = executeSearch(boolQuery(), settings)

    def matchingQuery(query: String, settings: SearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = {
      val language =
        if (settings.fallback) "*" else settings.searchLanguage

      val fullQuery = if (settings.exactTitleMatch) {
        boolQuery().must(simpleStringQuery(query).flags(SimpleQueryStringFlag.NONE).field(s"title.$language.lower"))
      } else {
        boolQuery().must(
          boolQuery()
            .should(
              List(
                simpleStringQuery(query).field(s"title.$language", 2),
                simpleStringQuery(query).field(s"content.$language", 1),
                simpleStringQuery(query).field(s"gloss", 1),
                idsQuery(query)
              ) ++
                buildNestedEmbedField(List(query), None, settings.searchLanguage, settings.fallback) ++
                buildNestedEmbedField(List.empty, Some(query), settings.searchLanguage, settings.fallback)
            )
        )
      }
      executeSearch(fullQuery, settings)
    }

    def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = {
      val idFilter      = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))
      val typeFilter    = settings.conceptType.map(ct => termsQuery("conceptType", ct))
      val subjectFilter = orFilter(settings.subjects, "subjectIds")
      val tagFilter     = languageOrFilter(settings.tagsToFilterBy, "tags", settings.searchLanguage, settings.fallback)

      val (languageFilter, searchLanguage) = settings.searchLanguage match {
        case "" | AllLanguages =>
          (None, "*")
        case lang =>
          if (settings.fallback)
            (None, "*")
          else
            (Some(existsQuery(s"title.$lang")), lang)
      }

      val embedResourceAndIdFilter =
        buildNestedEmbedField(settings.embedResource, settings.embedId, settings.searchLanguage, settings.fallback)

      val filters = List(
        idFilter,
        typeFilter,
        languageFilter,
        subjectFilter,
        tagFilter,
        embedResourceAndIdFilter
      )

      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new ResultWindowTooLargeException())
      } else {
        val aggregations = buildTermsAggregation(settings.aggregatePaths, List(publishedConceptIndexService.getMapping))
        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
            .trackTotalHits(true)
            .query(filteredSearch)
            .highlighting(highlight("*"))
            .aggs(aggregations)
            .sortBy(getSortDefinition(settings.sort, searchLanguage))

        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            val aggResult = getAggregationsFromResult(response.result)
            Success(
              SearchResult(
                totalCount = response.result.totalHits,
                page = Some(settings.page),
                pageSize = numResults,
                language = searchLanguage,
                results = getHits(response.result, settings.searchLanguage),
                aggregations = aggResult,
                scrollId = response.result.scrollId
              )
            )
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        publishedConceptIndexService.indexDocuments(None)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} concepts in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
