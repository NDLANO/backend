/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.{DraftStatus, RevisionStatus}
import no.ndla.common.model.domain.learningpath.EmbedType
import no.ndla.common.model.domain.{ArticleType, Availability, Priority}
import no.ndla.network.NdlaClient
import no.ndla.network.model.RequestInfo
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.ApiSearchException
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.model.domain.{ApiSearchResults, DomainDumpResults, LearningResourceType, SearchParams}
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import sttp.client3.quick._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.math.ceil
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient with StrictLogging with Props =>

  trait SearchApiClient {
    val name: String
    val baseUrl: String
    val searchPath: String
    val dumpDomainPath: String = s"intern/dump/$name"

    def getSingle[T](id: Long)(implicit mf: Manifest[T]): Try[T] = {
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

    def getChunks[T](implicit mf: Manifest[T]): Iterator[Try[Seq[T]]] = {
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

    private def getChunk[T](page: Int, pageSize: Int)(implicit mf: Manifest[T]): Try[DomainDumpResults[T]] = {
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

    def search(searchParams: SearchParams)(implicit executionContext: ExecutionContext): Future[Try[ApiSearchResults]]

    def get[T](path: String, params: Map[String, String], timeout: Int = 5000)(implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats =
        org.json4s.DefaultFormats +
          new EnumNameSerializer(LearningPathStatus) +
          new EnumNameSerializer(LearningPathVerificationStatus) +
          new EnumNameSerializer(StepType) +
          new EnumNameSerializer(StepStatus) +
          new EnumNameSerializer(EmbedType) +
          new EnumNameSerializer(LearningResourceType) +
          new EnumNameSerializer(Availability) +
          NDLADate.Json4sSerializer ++
          JavaTimeSerializers.all ++
          JavaTypesSerializers.all +
          Json4s.serializer(ArticleType) +
          Json4s.serializer(DraftStatus) +
          Json4s.serializer(RevisionStatus) +
          Json4s.serializer(Priority)
      val url     = s"$baseUrl/$path"
      val request = quickRequest.get(uri"$url?$params").readTimeout(timeout.millis)
      ndlaClient.fetchWithForwardedAuth[T](request, None)
    }

    protected def search[T <: ApiSearchResults](
        searchParams: SearchParams
    )(implicit mf: Manifest[T], executionContext: ExecutionContext): Future[Try[T]] = {
      val queryParams = searchParams.remaindingParams ++ Map(
        "language"  -> searchParams.language.getOrElse("*"),
        "sort"      -> searchParams.sort.entryName,
        "page"      -> searchParams.page.toString,
        "page-size" -> searchParams.pageSize.toString
      )

      Future { get(searchPath, queryParams) }.map {
        case Success(a)  => Success(a)
        case Failure(ex) => Failure(new ApiSearchException(name, ex.getMessage))
      }
    }

  }
}
