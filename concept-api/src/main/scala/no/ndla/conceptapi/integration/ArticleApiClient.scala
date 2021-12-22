/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.model.domain
import no.ndla.network.NdlaClient
import org.json4s.Formats
import scalaj.http.Http
import io.lemonlabs.uri.dsl._

import scala.math.ceil
import scala.util.{Failure, Success, Try}

case class ConceptDomainDumpResults(
    totalCount: Long,
    results: List[domain.Concept]
)

trait ArticleApiClient {
  this: NdlaClient with LazyLogging =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient {
    val baseUrl = s"http://${ConceptApiProperties.ArticleApiHost}/intern"
    val dumpDomainPath = "dump/concepts"

    def getChunks: Iterator[Try[Seq[domain.Concept]]] = {
      getChunk(0, 0) match {
        case Success(initSearch) =>
          val dbCount = initSearch.totalCount
          val pageSize = ConceptApiProperties.IndexBulkSize
          val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
          val pages = Seq.range(1, numPages + 1)

          val iterator: Iterator[Try[Seq[domain.Concept]]] = pages.iterator.map(p => {
            getChunk(p, pageSize).map(_.results)
          })

          iterator
        case Failure(ex) =>
          logger.error(s"Could not fetch initial chunk from $baseUrl/$dumpDomainPath")
          Iterator(Failure(ex))
      }
    }

    def get[T](path: String, params: Map[String, Any], timeout: Int)(implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
      ndlaClient.fetchWithForwardedAuth[T](Http((baseUrl / path).addParams(params.toList)).timeout(timeout, timeout))
    }

    private def getChunk(page: Int, pageSize: Int): Try[ConceptDomainDumpResults] = {
      val params = Map(
        "page" -> page,
        "page-size" -> pageSize
      )

      get[ConceptDomainDumpResults](dumpDomainPath, params, timeout = 20000) match {
        case Success(result) =>
          logger.info(s"Fetched chunk of ${result.results.size} concepts from ${baseUrl.addParams(params)}")
          Success(result)
        case Failure(ex) =>
          logger.error(
            s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'")
          Failure(ex)
      }
    }

  }

}
