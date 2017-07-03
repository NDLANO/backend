/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.ElasticClient
import no.ndla.audioapi.model.api.{AudioSummary, SearchResult, Title}
import no.ndla.audioapi.model.domain.NdlaSearchException
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.Language.{DefaultLanguage, NoLanguage, AllLanguages}
import no.ndla.network.ApplicationUrl
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, Operator, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, SortBuilders, SortOrder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: ElasticClient with SearchIndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    private val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("license", "copyrighted"))

    def getHits(response: JestSearchResult, language: String): Seq[AudioSummary] = {
      var resultList = Seq[AudioSummary]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitAsAudioSummary(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsAudioSummary(hit: JsonObject, language: String): AudioSummary = {
      import scala.collection.JavaConversions._

      val titles = hit.get("titles").getAsJsonObject.entrySet().to[Seq]
        .map(entr => Title(entr.getValue.getAsString, Some(entr.getKey)))

      val supportedLanguages = titles.map(_.language.getOrElse(NoLanguage))

      val title = titles
        .find(title => title.language.getOrElse(NoLanguage) == (if (language == AllLanguages) DefaultLanguage else language))
        .getOrElse(titles.head)
        .title

      AudioSummary(
        hit.get("id").getAsLong,
        title,
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.get("license").getAsString,
        supportedLanguages
      )
    }

    def all(language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      executeSearch(
        language.getOrElse(AllLanguages),
        license,
        sort,
        page,
        pageSize,
        QueryBuilders.boolQuery())
    }

    def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      val searchLanguage = language.getOrElse(DefaultLanguage)

      val titleSearch = QueryBuilders.matchQuery(s"titles.$searchLanguage", query.mkString(" ")).operator(Operator.AND)
      val tagSearch = QueryBuilders.matchQuery(s"tags.$searchLanguage", query.mkString(" ")).operator(Operator.AND)

      val fullSearch = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(QueryBuilders.nestedQuery("titles", titleSearch, ScoreMode.Avg))
          .should(QueryBuilders.nestedQuery("tags", tagSearch, ScoreMode.Avg)))

      executeSearch(searchLanguage, license, sort, page, pageSize, fullSearch)
    }

    def executeSearch(language: String, license: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int], queryBuilder: BoolQueryBuilder): SearchResult = {
      val (filteredSearch, searchLanguage) = {

        val licenseFilteredSearch = license match {
          case None => queryBuilder.filter(noCopyright)
          case Some(lic) => queryBuilder.filter(QueryBuilders.termQuery("license", lic))
        }

        language match {
          case AllLanguages => (licenseFilteredSearch, DefaultLanguage)
          case _ => (licenseFilteredSearch.filter(QueryBuilders.nestedQuery("titles", QueryBuilders.existsQuery(s"titles.$language"), ScoreMode.Avg)), language)
        }

      }

      val searchQuery = new SearchSourceBuilder().query(filteredSearch).sort(getSortDefinition(sort, searchLanguage))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(AudioApiProperties.SearchIndex)
        .setParameter(Parameters.SIZE, numResults)
        .setParameter("from", startAt)

      jestClient.execute(request.build()) match {
        case Success(response) => SearchResult(response.getTotal.toLong, page.getOrElse(1), numResults, language, getHits(response, language))
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortBuilder = {
      sort match {
        case (Sort.ByTitleAsc) => SortBuilders.fieldSort(s"titles.$language.raw").setNestedPath("titles").order(SortOrder.ASC).missing("_last").unmappedType("string")
        case (Sort.ByTitleDesc) => SortBuilders.fieldSort(s"titles.$language.raw").setNestedPath("titles").order(SortOrder.DESC).missing("_last").unmappedType("string")
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
      }
    }

    def countDocuments(): Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(AudioApiProperties.SearchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
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

      f onFailure { case t => logger.warn("Unable to create index: " + t.getMessage, t) }
      f onSuccess {
        case Success(reindexResult)  => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
