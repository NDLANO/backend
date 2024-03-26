/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.domain
import no.ndla.network.NdlaClient
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.math.ceil
import scala.util.{Failure, Success, Try}

case class ConceptDomainDumpResults(
    totalCount: Long,
    results: List[domain.Concept]
)

object ConceptDomainDumpResults {
  implicit val encoder: Encoder[ConceptDomainDumpResults] = deriveEncoder
  implicit val decoder: Decoder[ConceptDomainDumpResults] = deriveDecoder
}

trait ArticleApiClient {
  this: NdlaClient with StrictLogging with Props =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient {
    val baseUrl: String = s"http://${props.ArticleApiHost}/intern"
    val dumpDomainPath  = "dump/concepts"

    def getChunks(user: TokenUser): Iterator[Try[Seq[domain.Concept]]] = {
      getChunk(0, 0, user) match {
        case Success(initSearch) =>
          val dbCount  = initSearch.totalCount
          val pageSize = props.IndexBulkSize
          val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
          val pages    = Seq.range(1, numPages + 1)

          val iterator: Iterator[Try[Seq[domain.Concept]]] = pages.iterator.map(p => {
            getChunk(p, pageSize, user).map(_.results)
          })

          iterator
        case Failure(ex) =>
          logger.error(s"Could not fetch initial chunk from $baseUrl/$dumpDomainPath")
          Iterator(Failure(ex))
      }
    }

    def get[T: Decoder](path: String, params: Map[String, String], timeout: Int, user: TokenUser): Try[T] = {
      ndlaClient.fetchWithForwardedAuth[T](
        quickRequest.get(uri"$baseUrl/$path".withParams(params)).readTimeout(timeout.millis),
        Some(user)
      )
    }

    private def getChunk(page: Int, pageSize: Int, user: TokenUser): Try[ConceptDomainDumpResults] = {
      val params = Map(
        "page"      -> page.toString,
        "page-size" -> pageSize.toString
      )

      get[ConceptDomainDumpResults](dumpDomainPath, params, timeout = 20000, user) match {
        case Success(result) =>
          logger.info(s"Fetched chunk of ${result.results.size} concepts from ${baseUrl.addParams(params)}")
          Success(result)
        case Failure(ex) =>
          logger.error(
            s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'"
          )
          Failure(ex)
      }
    }

  }

}
