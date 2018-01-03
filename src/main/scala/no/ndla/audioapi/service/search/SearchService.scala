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
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, QueryDefinition}
import com.sksamuel.elastic4s.searches.sort.{SortDefinition, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.{Elastic4sClient, ElasticClient}
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
import scala.util.{Failure, Success}

trait SearchService {
  this: Elastic4sClient with ElasticClient with SearchIndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

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
      implicit val formats = DefaultFormats
      val hit = parse(hitString)

      val supportedLanguages = (hit \ "titles").extract[Map[String, _]].keySet.toSeq
      val titles = (hit \ "titles").extract[Map[String, String]].map(title => domain.Title(title._2, title._1)).toSeq
      val title = findByLanguageOrBestEffort(titles, Some(language)) match {
        case None => Title("", language)
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

    def all(language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      executeSearch(
        language,
        license,
        sort,
        page,
        pageSize,
        boolQuery())
    }

    def matchingQuery(query: String, language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      val fullSearch = boolQuery()
        .must(boolQuery()
          .should(
            languageSpecificSearch("titles", language, query, 1),
            languageSpecificSearch("tags", language, query, 1))
        )

      executeSearch(language, license, sort, page, pageSize, fullSearch)
    }

    private def languageSpecificSearch(searchField: String, language: Option[String], query: String, boost: Float): QueryDefinition = {
      language match {
        case None | Some(Language.AllLanguages) | Some("*") =>
          val searchQuery = simpleStringQuery(query).field(s"$searchField.*", 1)
          nestedQuery(searchField, searchQuery).boost(boost)
        case Some(lang) =>
          val searchQuery = simpleStringQuery(query).field(s"$searchField.$lang", 1)
          nestedQuery(searchField, searchQuery).boost(boost)
      }
    }

    def executeSearch(language: Option[String], license: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int], queryBuilder: BoolQueryDefinition): SearchResult = {

      val licenseFilter = license match {
        case None => Some(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
      }

      val (languageFilter, searchLanguage) = language match {
        case None | Some(Language.AllLanguages) => (None, "*")
        case Some(lang) => (Some(nestedQuery("titles", existsQuery(s"titles.$lang")).scoreMode(ScoreMode.Avg)), lang)
      }


      val filters = List(licenseFilter, languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)


      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = page.getOrElse(1)*numResults
      if(requestedResultWindow > AudioApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${AudioApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException()
      }

      e4sClient.execute{
        search(AudioApiProperties.SearchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .sortBy(getSortDefinition(sort, searchLanguage))

      } match {
        case Success(response) =>
          SearchResult(response.result.totalHits, page.getOrElse(1), numResults, searchLanguage, getHits(response.result, searchLanguage))
        case Failure(ex) =>
          errorHandler(Failure(ex))
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case Language.NoLanguage | Language.AllLanguages => "*"
        case _ => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.ASC).missing("_last")
            case _ => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.DESC).missing("_last")
            case _ => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByRelevanceAsc) => fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc) => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc) => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def countDocuments: Long = {
      val response = e4sClient.execute{
        catCount(AudioApiProperties.SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_) => 0
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
        case None => 0
      }

      (startAt, numResults)
    }

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) => {
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${AudioApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${AudioApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getResponse.getErrorMessage)
              println(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${AudioApiProperties.SearchIndex}", e.getResponse.getErrorMessage)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult)  => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
