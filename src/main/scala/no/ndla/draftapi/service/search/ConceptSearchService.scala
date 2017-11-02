/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import java.util.Map.Entry

import com.google.gson.{JsonElement, JsonObject}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.Search
import io.searchbox.params.Parameters
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.integration.ElasticClient
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.service.ConverterService
import no.ndla.draftapi.service.search.ConceptIndexService
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ConceptSearchService {
  this: ElasticClient with SearchService with ConceptIndexService with ConverterService =>
  val conceptSearchService: ConceptSearchService

  class ConceptSearchService extends LazyLogging with SearchService[api.ConceptSummary] {
    override val searchIndex: String = DraftApiProperties.ConceptSearchIndex

    private def getSearchLanguage(supportedLanguages: Seq[String], language: String): String = {
      language match {
        case Language.NoLanguage if supportedLanguages.contains(Language.DefaultLanguage) => Language.DefaultLanguage
        case Language.NoLanguage if supportedLanguages.nonEmpty => supportedLanguages.head
        case lang => lang
      }
    }

    override def hitToApiModel(hit: JsonObject, language: String): api.ConceptSummary = {
      val titles = getEntrySetSeq(hit, "title").map(ent => ConceptTitle(ent.getValue.getAsString, ent.getKey))
      val contents = getEntrySetSeq(hit, "content").map(ent => ConceptContent(ent.getValue.getAsString, ent.getKey))
      val supportedLanguages = (titles union contents).map(_.language).toSet

      val title = Language.findByLanguageOrBestEffort(titles, language).map(converterService.toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.DefaultLanguage))
      val concept = Language.findByLanguageOrBestEffort(contents, language).map(converterService.toApiConceptContent).getOrElse(api.ConceptContent("", Language.DefaultLanguage))

      api.ConceptSummary(
        hit.get("id").getAsLong,
        title,
        concept,
        supportedLanguages
      )
    }

    def getEntrySetSeq(hit: JsonObject, fieldPath: String): Seq[Entry[String, JsonElement]] = {
      hit.get(fieldPath).getAsJsonObject.entrySet.asScala.to[Seq]
    }

    def all(withIdIn: List[Long], language: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      executeSearch(withIdIn, language, sort, page, pageSize, QueryBuilders.boolQuery())
    }

    def matchingQuery(query: String, withIdIn: List[Long], searchLanguage: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      val titleSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"title.$searchLanguage")
      val contentSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"content.$searchLanguage")

      val fullQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(QueryBuilders.nestedQuery("title", titleSearch, ScoreMode.Avg).boost(2))
          .should(QueryBuilders.nestedQuery("content", contentSearch, ScoreMode.Avg).boost(1)))

      executeSearch(withIdIn, searchLanguage, sort, page, pageSize, fullQuery)
    }

    def executeSearch(withIdIn: List[Long], language: String, sort: Sort.Value, page: Int, pageSize: Int, queryBuilder: BoolQueryBuilder): api.ConceptSearchResult = {
      val idFilteredSearch = withIdIn.nonEmpty match {
        case true => queryBuilder.filter(QueryBuilders.idsQuery(DraftApiProperties.ConceptSearchDocument).addIds(withIdIn.map(_.toString):_*))
        case false => queryBuilder
      }
      val searchQuery = new SearchSourceBuilder().query(idFilteredSearch).sort(getSortDefinition(sort, language))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(searchIndex)
        .setParameter(Parameters.SIZE, numResults) .setParameter("from", startAt)


      jestClient.execute(request.build()) match {
        case Success(response) => api.ConceptSearchResult(response.getTotal.toLong, page, numResults, getHits(response, language))
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    protected def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) =>
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex")
            case _ =>
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in $searchIndex", e.getResponse.getErrorMessage)
          }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        conceptIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
