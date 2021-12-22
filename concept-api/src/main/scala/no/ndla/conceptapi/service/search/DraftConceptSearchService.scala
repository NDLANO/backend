/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.BoolQuery
import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.integration.Elastic4sClient
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.{OperationNotAllowedException, ResultWindowTooLargeException, SubjectTags}
import no.ndla.conceptapi.model.domain.{Language, SearchResult}
import no.ndla.conceptapi.model.search.DraftSearchSettings
import no.ndla.conceptapi.service.ConverterService

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait DraftConceptSearchService {
  this: Elastic4sClient
    with SearchService
    with DraftConceptIndexService
    with ConverterService
    with SearchConverterService =>
  val draftConceptSearchService: DraftConceptSearchService

  class DraftConceptSearchService extends LazyLogging with SearchService[api.ConceptSummary] {
    override val searchIndex: String = ConceptApiProperties.DraftConceptSearchIndex

    override def hitToApiModel(hitString: String, language: String): api.ConceptSummary =
      searchConverterService.hitAsConceptSummary(hitString, language)

    def getTagsWithSubjects(subjectIds: List[String],
                            language: String,
                            fallback: Boolean): Try[List[api.SubjectTags]] = {
      if (subjectIds.size <= 0) {
        Failure(OperationNotAllowedException("Will not generate list of subject tags with no specified subjectIds"))
      } else {
        implicit val ec: ExecutionContextExecutor =
          ExecutionContext.fromExecutor(Executors.newFixedThreadPool(subjectIds.size))
        val searches = subjectIds.traverse(subjectId => searchSubjectIdTags(subjectId, language, fallback))
        Await.result(searches, 1 minute).sequence.map(_.flatten)
      }
    }

    private def searchSubjectIdTags(subjectId: String, language: String, fallback: Boolean)(
        implicit executor: ExecutionContext): Future[Try[List[SubjectTags]]] =
      Future {
        val settings = DraftSearchSettings.empty.copy(
          subjects = Set(subjectId),
          searchLanguage = language,
          fallback = fallback,
          shouldScroll = true
        )

        searchUntilNoMoreResults(settings).map(searchResults => {
          val tagsInSubject = for {
            searchResult <- searchResults
            searchHits <- searchResult.results
            matchedTags <- searchHits.tags.toSeq
          } yield matchedTags

          searchConverterService
            .groupSubjectTagsByLanguage(subjectId, tagsInSubject)
            .filter(tags => tags.language == language || language == Language.AllLanguages || fallback)
        })
      }

    @tailrec
    private def searchUntilNoMoreResults(
        searchSettings: DraftSearchSettings,
        prevResults: List[SearchResult[api.ConceptSummary]] = List.empty
    ): Try[List[SearchResult[api.ConceptSummary]]] = {
      val page = prevResults.lastOption.flatMap(_.page).getOrElse(0) + 1

      val result = prevResults.lastOption.flatMap(_.scrollId) match {
        case Some(scrollId) => this.scroll(scrollId, searchSettings.searchLanguage)
        case None           => this.all(searchSettings.copy(page = page))
      }

      result match {
        case Failure(ex)                                                        => Failure(ex)
        case Success(value) if value.results.size <= 0 || value.totalCount == 0 => Success(prevResults)
        case Success(value)                                                     => searchUntilNoMoreResults(searchSettings, prevResults :+ value)
      }
    }

    def all(settings: DraftSearchSettings): Try[SearchResult[api.ConceptSummary]] = executeSearch(boolQuery(), settings)

    def matchingQuery(query: String, settings: DraftSearchSettings): Try[SearchResult[api.ConceptSummary]] = {
      val language =
        if (settings.searchLanguage == Language.AllLanguages || settings.fallback) "*" else settings.searchLanguage

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              List(
                simpleStringQuery(query).field(s"title.$language", 2),
                simpleStringQuery(query).field(s"content.$language", 1),
                simpleStringQuery(query).field(s"tags.$language", 1),
                idsQuery(query)
              ) ++
                buildNestedEmbedField(Some(query), None, settings.searchLanguage, settings.fallback) ++
                buildNestedEmbedField(None, Some(query), settings.searchLanguage, settings.fallback)
            )
        )

      executeSearch(fullQuery, settings)
    }

    def executeSearch(queryBuilder: BoolQuery, settings: DraftSearchSettings): Try[SearchResult[api.ConceptSummary]] = {
      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))
      val statusFilter = orFilter(settings.statusFilter, "status.current", "status.other")
      val subjectFilter = orFilter(settings.subjects, "subjectIds")
      val tagFilter = languageOrFilter(settings.tagsToFilterBy, "tags", settings.searchLanguage, settings.fallback)
      val userFilter = orFilter(settings.userFilter, "updatedBy")

      val (languageFilter, searchLanguage) = settings.searchLanguage match {
        case "" | Language.AllLanguages => (None, "*")
        case _ if settings.fallback     => (None, "*")
        case lang                       => (Some(existsQuery(s"title.$lang")), lang)
      }

      val embedResourceAndIdFilter =
        buildNestedEmbedField(settings.embedResource, settings.embedId, settings.searchLanguage, settings.fallback)

      val filters =
        List(
          idFilter,
          languageFilter,
          subjectFilter,
          tagFilter,
          statusFilter,
          userFilter,
          embedResourceAndIdFilter
        )

      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ConceptApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${ConceptApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException())
      } else {
        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
            .query(filteredSearch)
            .highlighting(highlight("*"))
            .sortBy(getSortDefinition(settings.sort, searchLanguage))

        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ConceptApiProperties.ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(settings.page),
                numResults,
                searchLanguage,
                getHits(response.result, settings.searchLanguage),
                response.result.scrollId
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        draftConceptIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} concepts in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
