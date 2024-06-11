/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.network.NdlaClient
import no.ndla.network.model.RequestInfo
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.domain.DomainDumpResults
import sttp.client3.quick.*

import scala.concurrent.duration.*
import scala.math.ceil
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient & StrictLogging & Props =>

  trait SearchApiClient {
    val name: String
    val baseUrl: String
    val searchPath: String
    val dumpDomainPath: String = s"intern/dump/$name"

    def getSingle[T: Decoder](id: Long): Try[T] = {
      val path = s"$dumpDomainPath/$id"
      get[T](path, Map.empty, timeout = 120000) match {
        case Failure(ex) =>
          logger.error(
            s"Could not fetch single $name (id: $id) from '$baseUrl/$path'"
          )
          Failure(ex)
        case Success(value) => Success(value)
      }
    }

    def getChunks[T: Decoder]: Iterator[Try[Seq[T]]] = {
      val initial = getChunk(0, 0)

      initial match {
        case Success(initSearch) =>
          val dbCount  = initSearch.totalCount
          val pageSize = props.IndexBulkSize
          val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
          val pages    = Seq.range(1, numPages + 1)

          val iterator: Iterator[Try[Seq[T]]] = pages.iterator.map(p => {
            getChunk[T](p, pageSize).map(_.results)
          })

          iterator
        case Failure(ex) =>
          logger.error(s"Could not fetch initial chunk from $baseUrl/$dumpDomainPath")
          Iterator(Failure(ex))
      }
    }

    private def getChunk[T: Decoder](page: Int, pageSize: Int): Try[DomainDumpResults[T]] = {
      val params = Map(
        "page"      -> page.toString,
        "page-size" -> pageSize.toString
      )
      val reqs = RequestInfo.fromThreadContext()
      reqs.setThreadContextRequestInfo()
      get[DomainDumpResults[T]](dumpDomainPath, params, timeout = 120000) match {
        case Success(result) =>
          logger.info(s"Fetched chunk of ${result.results.size} $name from ${baseUrl.addParams(params)}")
          Success(result)
        case Failure(ex) =>
          logger.error(
            s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'"
          )
          Failure(ex)
      }
    }

    def get[T: Decoder](path: String, params: Map[String, String], timeout: Int = 5000): Try[T] = {
      val url     = s"$baseUrl/$path"
      val request = quickRequest.get(uri"$url?$params").readTimeout(timeout.millis)
      ndlaClient.fetchWithForwardedAuth[T](request, None)
    }
  }
}
