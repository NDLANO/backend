/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.dsl._
import no.ndla.network.NdlaClient
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.model.api.ApiSearchException
import no.ndla.searchapi.model.domain.article.{Availability, LearningResourceType}
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.model.domain.{ApiSearchResults, DomainDumpResults, RequestInfo, SearchParams}
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import scalaj.http.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.math.ceil
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient with LazyLogging =>

  trait SearchApiClient {
    val name: String
    val baseUrl: String
    val searchPath: String
    val dumpDomainPath: String = s"intern/dump/$name"

    def getChunks[T](implicit mf: Manifest[T], ec: ExecutionContext): Iterator[Future[Try[Seq[T]]]] = {
      val fut = getChunk(0, 0)
      val initial = Await.result(fut, 10.minutes)

      initial match {
        case Success(initSearch) =>
          val dbCount = initSearch.totalCount
          val pageSize = SearchApiProperties.IndexBulkSize
          val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
          val pages = Seq.range(1, numPages + 1)

          val iterator: Iterator[Future[Try[Seq[T]]]] = pages.iterator.map(p => {
            getChunk[T](p, pageSize).map(_.map(_.results))
          })

          iterator
        case Failure(ex) =>
          logger.error(s"Could not fetch initial chunk from $baseUrl/$dumpDomainPath")
          Iterator(Future(Failure(ex)))
      }
    }

    private def getChunk[T](page: Int, pageSize: Int)(implicit mf: Manifest[T],
                                                      ec: ExecutionContext): Future[Try[DomainDumpResults[T]]] = {
      val params = Map(
        "page" -> page,
        "page-size" -> pageSize
      )
      val reqs = RequestInfo()
      Future {
        reqs.setRequestInfo()
        get[DomainDumpResults[T]](dumpDomainPath, params, timeout = 120000) match {
          case Success(result) =>
            logger.info(s"Fetched chunk of ${result.results.size} $name from ${baseUrl.addParams(params)}")
            Success(result)
          case Failure(ex) =>
            logger.error(
              s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'")
            Failure(ex)
        }
      }
    }

    def search(searchParams: SearchParams)(implicit executionContext: ExecutionContext): Future[Try[ApiSearchResults]]

    def get[T](path: String, params: Map[String, Any], timeout: Int = 5000)(implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats =
        org.json4s.DefaultFormats +
          new EnumNameSerializer(ArticleStatus) +
          new EnumNameSerializer(LearningPathStatus) +
          new EnumNameSerializer(LearningPathVerificationStatus) +
          new EnumNameSerializer(StepType) +
          new EnumNameSerializer(StepStatus) +
          new EnumNameSerializer(EmbedType) +
          new EnumNameSerializer(LearningResourceType) +
          new EnumNameSerializer(Availability) ++
          org.json4s.ext.JodaTimeSerializers.all

      ndlaClient.fetchWithForwardedAuth[T](Http((baseUrl / path).addParams(params.toList)).timeout(timeout, timeout))
    }

    protected def search[T <: ApiSearchResults](
        searchParams: SearchParams)(implicit mf: Manifest[T], executionContext: ExecutionContext): Future[Try[T]] = {
      val queryParams = searchParams.remaindingParams ++ Map("language" -> searchParams.language.getOrElse("*"),
                                                             "sort" -> searchParams.sort,
                                                             "page" -> searchParams.page,
                                                             "page-size" -> searchParams.pageSize)

      Future { get(searchPath, queryParams) }.map {
        case Success(a)  => Success(a)
        case Failure(ex) => Failure(new ApiSearchException(name, ex.getMessage))
      }
    }

  }
}
