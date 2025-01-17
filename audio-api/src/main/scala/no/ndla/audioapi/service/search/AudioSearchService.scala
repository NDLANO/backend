/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.audioapi.model.domain.SearchSettings
import no.ndla.audioapi.model.search.SearchableAudioInformation
import no.ndla.audioapi.model.{api, domain}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.language.Language.AllLanguages
import no.ndla.search.Elastic4sClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait AudioSearchService {
  this: Elastic4sClient
    with AudioIndexService
    with SearchConverterService
    with SearchService
    with Props
    with ErrorHandling =>
  val audioSearchService: AudioSearchService

  class AudioSearchService extends StrictLogging with SearchService[api.AudioSummaryDTO] {
    import props._

    override val searchIndex: String = SearchIndex

    override def hitToApiModel(hitString: String, language: String): Try[api.AudioSummaryDTO] = {
      for {
        searchable <- CirceUtil.tryParseAs[SearchableAudioInformation](hitString)
        result     <- searchConverterService.asAudioSummary(searchable, language)
      } yield result
    }

    def matchingQuery(settings: SearchSettings): Try[domain.SearchResult[api.AudioSummaryDTO]] = {

      val fullSearch = settings.query.emptySomeToNone match {
        case Some(query) =>
          val languageSearch = (field: String, boost: Double) =>
            languageSpecificSearch(
              field,
              settings.language,
              query,
              boost,
              fallback = settings.fallback
            )

          boolQuery()
            .must(
              boolQuery()
                .should(
                  languageSearch("titles", 2),
                  languageSearch("tags", 1),
                  languageSearch("manuscript", 1),
                  languageSearch("podcastMetaIntroduction", 1),
                  idsQuery(query)
                )
            )
        case None => boolQuery()
      }

      executeSearch(settings, fullSearch)
    }

    def executeSearch(
        settings: SearchSettings,
        queryBuilder: BoolQuery
    ): Try[domain.SearchResult[api.AudioSummaryDTO]] = {

      val licenseFilter = settings.license match {
        case None      => Some(boolQuery().not(termQuery("license", "copyrighted")))
        case Some(lic) => Some(termQuery("license", lic))
      }

      val seriesEpisodeFilter = settings.seriesFilter match {
        case Some(true)  => Some(nestedQuery("series", existsQuery("series")))
        case Some(false) => Some(not(nestedQuery("series", existsQuery("series"))))
        case None        => None
      }

      val (languageFilter, searchLanguage) = settings.language match {
        case None | Some(AllLanguages) => (None, "*")
        case Some(lang) =>
          if (settings.fallback) (None, lang)
          else (Some(existsQuery(s"titles.$lang")), lang)
      }

      val audioTypeFilter = settings.audioType match {
        case Some(audioType) => Some(termQuery("audioType", audioType.toString))
        case None            => None
      }

      val filters        = List(licenseFilter, languageFilter, audioTypeFilter, seriesEpisodeFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new Helpers.ResultWindowTooLargeException())
      } else {

        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
            .trackTotalHits(true)
            .query(filteredSearch)
            .highlighting(highlight("*"))
            .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            getHits(response.result, searchLanguage).map(results =>
              domain.SearchResult(
                response.result.totalHits,
                Some(settings.page.getOrElse(1)),
                numResults,
                searchLanguage,
                results,
                response.result.scrollId
              )
            )
          case Failure(ex) => errorHandler(ex)
        }
      }

    }

    protected override def scheduleIndexDocuments(): Unit = {
      val f = Future {
        audioIndexService.indexDocuments(None)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
