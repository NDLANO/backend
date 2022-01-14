/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties.{
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  SearchIndex
}
import no.ndla.audioapi.model.api.ResultWindowTooLargeException
import no.ndla.audioapi.model.domain.SearchSettings
import no.ndla.audioapi.model.search.SearchableAudioInformation
import no.ndla.audioapi.model.{api, domain}
import no.ndla.search.Elastic4sClient
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s._
import org.json4s.native.Serialization

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait AudioSearchService {
  this: Elastic4sClient with AudioIndexService with SearchConverterService with SearchService =>
  val audioSearchService: AudioSearchService

  class AudioSearchService extends LazyLogging with SearchService[api.AudioSummary] {

    override val searchIndex: String = SearchIndex

    override def hitToApiModel(hitString: String, language: String): Try[api.AudioSummary] = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchable = Serialization.read[SearchableAudioInformation](hitString)
      searchConverterService.asAudioSummary(searchable, language)
    }

    def matchingQuery(settings: SearchSettings): Try[domain.SearchResult[api.AudioSummary]] = {

      val fullSearch = settings.query match {
        case Some(query) =>
          boolQuery()
            .must(
              boolQuery()
                .should(
                  languageSpecificSearch("titles", settings.language, query, 2),
                  languageSpecificSearch("tags", settings.language, query, 1),
                  languageSpecificSearch("manuscript", settings.language, query, 1),
                  languageSpecificSearch("podcastMetaIntroduction", settings.language, query, 1),
                  idsQuery(query)
                )
            )
        case None => boolQuery()
      }

      executeSearch(settings, fullSearch)
    }

    def executeSearch(settings: SearchSettings, queryBuilder: BoolQuery): Try[domain.SearchResult[api.AudioSummary]] = {

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
        case Some(lang) => (Some(existsQuery(s"titles.$lang")), lang)
        case _          => (None, "*")
      }

      val audioTypeFilter = settings.audioType match {
        case Some(audioType) => Some(termQuery("audioType", audioType.toString))
        case None            => None
      }

      val filters = List(licenseFilter, languageFilter, audioTypeFilter, seriesEpisodeFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException())
      } else {

        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
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
            getHits(response.result, searchLanguage).map(
              results =>
                domain.SearchResult(
                  response.result.totalHits,
                  Some(settings.page.getOrElse(1)),
                  numResults,
                  searchLanguage,
                  results,
                  response.result.scrollId
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }

    }

    protected override def scheduleIndexDocuments(): Unit = {
      val f = Future {
        audioIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
