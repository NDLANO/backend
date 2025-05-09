/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.ErrorHandling
import no.ndla.draftapi.model.domain.*
import no.ndla.language.Language
import no.ndla.mapping.License
import no.ndla.search.Elastic4sClient

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait ArticleSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService
    with SearchConverterService
    with Props
    with ErrorHandling =>
  val articleSearchService: ArticleSearchService

  class ArticleSearchService extends StrictLogging with SearchService[api.ArticleSummaryDTO] {
    import props.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}

    private val noCopyright = boolQuery().not(termQuery("license", License.Copyrighted.toString))

    override val searchIndex: String = props.DraftSearchIndex

    override def hitToApiModel(hit: String, language: String): api.ArticleSummaryDTO =
      searchConverterService.hitAsArticleSummary(hit, language)

    def matchingQuery(settings: SearchSettings): Try[SearchResult[api.ArticleSummaryDTO]] = {

      val fullQuery = settings.query match {
        case Some(query) =>
          val language =
            if (settings.fallback) "*" else settings.searchLanguage
          val titleSearch         = simpleStringQuery(query).field(s"title.$language", 6)
          val introSearch         = simpleStringQuery(query).field(s"introduction.$language", 2)
          val contentSearch       = simpleStringQuery(query).field(s"content.$language", 1)
          val tagSearch           = simpleStringQuery(query).field(s"tags.$language", 2)
          val notesSearch         = simpleStringQuery(query).field("notes", 1)
          val previousNotesSearch = simpleStringQuery(query).field("previousNotes", 1)

          boolQuery()
            .must(
              boolQuery()
                .should(
                  titleSearch,
                  introSearch,
                  contentSearch,
                  tagSearch,
                  notesSearch,
                  previousNotesSearch
                )
            )
        case None => boolQuery()
      }

      executeSearch(settings, fullQuery)
    }

    def executeSearch(settings: SearchSettings, queryBuilder: BoolQuery): Try[SearchResult[api.ArticleSummaryDTO]] = {

      val articleTypesFilter =
        if (settings.articleTypes.nonEmpty) Some(constantScoreQuery(termsQuery("articleType", settings.articleTypes)))
        else None

      val licenseFilter = settings.license match {
        case Some("all") => None
        case Some(lic)   => Some(termQuery("license", lic))
        case None        => Some(noCopyright)
      }

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val (languageFilter, searchLanguage) = settings.searchLanguage match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          if (settings.fallback)
            (None, "*")
          else
            (Some(existsQuery(s"title.$lang")), lang)
      }

      val grepCodesFilter =
        if (settings.grepCodes.nonEmpty) Some(constantScoreQuery(termsQuery("grepCodes", settings.grepCodes))) else None

      val filters        = List(licenseFilter, idFilter, languageFilter, articleTypesFilter, grepCodesFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .trackTotalHits(true)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(settings.sort, searchLanguage))

        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
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
              )
            )
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        articleIndexService.indexDocuments(None)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} articles in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
