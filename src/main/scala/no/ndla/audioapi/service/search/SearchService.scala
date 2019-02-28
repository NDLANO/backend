/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.searches.sort.SortOrder
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.AudioApiProperties.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.Language._
import no.ndla.audioapi.model.api.{AudioSummary, ResultWindowTooLargeException, SearchResult, Title}
import no.ndla.audioapi.model.domain.NdlaSearchException
import no.ndla.audioapi.model.{Language, Sort, domain}
import no.ndla.network.ApplicationUrl
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with SearchIndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    def scroll(scrollId: String, language: String): Try[domain.SearchResult] =
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHits(response.result, language)

          domain.SearchResult(
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = if (language == "*") Language.AllLanguages else language,
            results = hits,
            scrollId = response.result.scrollId
          )
        })

    def getHits(response: SearchResponse, language: String): Seq[AudioSummary] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }
            val hitString = result.sourceAsString
            hitAsAudioSummary(hitString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def hitAsAudioSummary(hitString: String, language: String): AudioSummary = {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val hit = parse(hitString)

      val titles = (hit \ "titles").extract[Map[String, String]].map(title => domain.Title(title._2, title._1)).toSeq
      val supportedLanguages = getSupportedLanguages(titles)
      val title = findByLanguageOrBestEffort(titles, Some(language)) match {
        case None    => Title("", language)
        case Some(x) => Title(x.title, x.language)
      }
      val id = (hit \ "id").extract[String].toLong

      AudioSummary(
        id,
        title,
        ApplicationUrl.get + (hit \ "id").extract[String],
        (hit \ "license").extract[String],
        supportedLanguages
      )
    }

    def all(language: Option[String],
            license: Option[String],
            page: Option[Int],
            pageSize: Option[Int],
            sort: Sort.Value): Try[domain.SearchResult] = {
      executeSearch(language, license, sort, page, pageSize, boolQuery())
    }

    def matchingQuery(query: String,
                      language: Option[String],
                      license: Option[String],
                      page: Option[Int],
                      pageSize: Option[Int],
                      sort: Sort.Value): Try[domain.SearchResult] = {
      val fullSearch = boolQuery()
        .must(
          boolQuery()
            .should(languageSpecificSearch("titles", language, query, 2),
                    languageSpecificSearch("tags", language, query, 1),
                    idsQuery(query)))

      executeSearch(language, license, sort, page, pageSize, fullSearch)
    }

    private def languageSpecificSearch(searchField: String,
                                       language: Option[String],
                                       query: String,
                                       boost: Float): Query = {
      language match {
        case None | Some(Language.AllLanguages) | Some("*") =>
          val searchQuery = simpleStringQuery(query).field(s"$searchField.*", 1)
          nestedQuery(searchField, searchQuery).scoreMode(ScoreMode.Avg).boost(boost)
        case Some(lang) =>
          val searchQuery = simpleStringQuery(query).field(s"$searchField.$lang", 1)
          nestedQuery(searchField, searchQuery).scoreMode(ScoreMode.Avg).boost(boost)
      }
    }

    def executeSearch(language: Option[String],
                      license: Option[String],
                      sort: Sort.Value,
                      page: Option[Int],
                      pageSize: Option[Int],
                      queryBuilder: BoolQuery): Try[domain.SearchResult] = {

      val licenseFilter = license match {
        case None      => Some(boolQuery().not(termQuery("license", "copyrighted")))
        case Some(lic) => Some(termQuery("license", lic))
      }

      val (languageFilter, searchLanguage) = language match {
        case None | Some(Language.AllLanguages) => (None, "*")
        case Some(lang)                         => (Some(nestedQuery("titles", existsQuery(s"titles.$lang")).scoreMode(ScoreMode.Avg)), lang)
      }

      val filters = List(licenseFilter, languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException())
      } else {

        val searchToExecute =
          search(AudioApiProperties.SearchIndex)
            .size(numResults)
            .from(startAt)
            .query(filteredSearch)
            .highlighting(highlight("*"))
            .sortBy(getSortDefinition(sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              domain.SearchResult(
                response.result.totalHits,
                Some(page.getOrElse(1)),
                numResults,
                if (searchLanguage == "*") Language.AllLanguages else searchLanguage,
                getHits(response.result, searchLanguage),
                response.result.scrollId
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }

    }

    private def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case Language.NoLanguage | Language.AllLanguages => "*"
        case _                                           => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.ASC).missing("_last")
            case _   => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.ASC).missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.DESC).missing("_last")
            case _   => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.DESC).missing("_last")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.ASC)
        case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.DESC)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def countDocuments: Long = {
      val response = e4sClient.execute {
        catCount(AudioApiProperties.SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(AudioApiProperties.MaxPageSize) else AudioApiProperties.DefaultPageSize
        case None => AudioApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None     => 0
      }

      (startAt, numResults)
    }

    private def errorHandler[T](exception: Throwable): Failure[T] = {
      exception match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index ${AudioApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              Failure(
                new IndexNotFoundException(s"Index ${AudioApiProperties.SearchIndex} not found. Scheduling a reindex"))
            case _ =>
              logger.error(e.getMessage)
              println(e.getMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${AudioApiProperties.SearchIndex}",
                                               e.getMessage)
          }
        case ex => Failure(ex)
      }
    }

    private def scheduleIndexDocuments(): Unit = {
      val f = Future {
        searchIndexService.indexDocuments
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
