/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.{SearchResult, Sort}
import no.ndla.language.Language
import no.ndla.search.{Elastic4sClient, IndexNotFoundException, NdlaSearchException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import cats.implicits.*

trait SearchService {
  this: Elastic4sClient & IndexService & SearchConverterService & Props =>

  trait SearchService[T] extends StrictLogging {
    val searchIndex: String
    val indexService: IndexService[?, ?]

    def hitToApiModel(hit: String, language: String): Try[T]

    def scroll(scrollId: String, language: String): Try[SearchResult[T]] =
      e4sClient
        .execute {
          searchScroll(scrollId, props.ElasticSearchScrollKeepAlive)
        }
        .flatMap(response => {
          getHits(response.result, language).map(hits =>
            SearchResult(
              totalCount = response.result.totalHits,
              page = None,
              pageSize = response.result.hits.hits.length,
              language = language,
              results = hits,
              scrollId = response.result.scrollId
            )
          )
        })

    def createEmptyIndexIfNoIndexesExist(): Unit = {
      val noIndexesExist =
        indexService.findAllIndexes(searchIndex).map(_.isEmpty).getOrElse(true)
      if (noIndexesExist) {
        indexService.createIndexWithGeneratedName match {
          case Success(_) =>
            logger.info("Created empty index")
            scheduleIndexDocuments()
          case Failure(f) =>
            logger.error(s"Failed to create empty index: $f")
        }
      } else {
        logger.info("Existing index(es) kept intact")
      }
    }

    def getHits(response: SearchResponse, language: String): Try[Seq[T]] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList
          resultArray.traverse(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages => searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _                     => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
          })
        case _ => Success(Seq())
      }
    }

    def getSortDefinition(sort: Sort, language: String): FieldSort

    def countDocuments(): Long = {
      val response = e4sClient.execute {
        catCount(searchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    protected def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(props.MaxPageSize) else props.DefaultPageSize
        case None => props.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None     => 0
      }

      (startAt, numResults)
    }

    protected def errorHandler[E](exception: Throwable): Failure[E] = {
      exception match {
        case e: NdlaSearchException[?] =>
          e.rf.map(_.status).getOrElse(0) match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index ${props.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              Failure(
                IndexNotFoundException(s"Index ${props.SearchIndex} not found. Scheduling a reindex")
              )
            case _ =>
              logger.error(e.getMessage)
              Failure(NdlaSearchException(s"Unable to execute search in ${props.SearchIndex}", e))
          }
        case t => Failure(t)
      }
    }

    private def scheduleIndexDocuments(): Unit = {
      val f = Future(indexService.indexDocuments(None))

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents ($searchIndex) in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }
}
