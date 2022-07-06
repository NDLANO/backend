/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.LazyLogging
import enumeratum.Json4s
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.native.Serialization.write
import scalaj.http.Http

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient with ConverterService with Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient(SearchApiBaseUrl: String = s"http://${props.SearchApiHost}") extends LazyLogging {

    private val InternalEndpoint        = s"$SearchApiBaseUrl/intern"
    private val SearchEndpoint          = s"$SearchApiBaseUrl/search-api/v1/search/editorial/"
    private val SearchEndpointPublished = s"$SearchApiBaseUrl/search-api/v1/search/"
    private val indexTimeout            = 1000 * 60

    implicit val formats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(ArticleStatus) +
        new EnumNameSerializer(Availability) +
        Json4s.serializer(ArticleType) +
        Json4s.serializer(RevisionStatus) ++
        JavaTimeSerializers.all ++
        JavaTypesSerializers.all

    def indexDraft(draft: Article): Article = {
      implicit val executionContext: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)

      val future = postWithData[Article, Article](s"$InternalEndpoint/draft/", draft)
      future.onComplete {
        case Success(Success(_)) =>
          logger.info(
            s"Successfully indexed draft with id: '${draft.id.getOrElse(-1)}' and revision '${draft.revision.getOrElse(-1)}' in search-api"
          )
        case Failure(e) =>
          logger.error(
            s"Failed to indexed draft with id: '${draft.id.getOrElse(-1)}' and revision '${draft.revision.getOrElse(-1)}' in search-api",
            e
          )
        case Success(Failure(e)) =>
          logger.error(
            s"Failed to indexed draft with id: '${draft.id.getOrElse(-1)}' and revision '${draft.revision.getOrElse(-1)}' in search-api",
            e
          )
      }

      draft
    }

    private def postWithData[A, B <: AnyRef](endpointUrl: String, data: B, params: (String, String)*)(implicit
        mf: Manifest[A],
        formats: org.json4s.Formats,
        executionContext: ExecutionContext
    ): Future[Try[A]] = {

      Future {
        ndlaClient.fetchWithForwardedAuth[A](
          Http(endpointUrl)
            .postData(write(data))
            .timeout(indexTimeout, indexTimeout)
            .method("POST")
            .params(params.toMap)
            .header("content-type", "application/json")
        )
      }
    }

    def draftsWhereUsed(articleId: Long): Seq[SearchHit] = {
      get[SearchResults](
        SearchEndpoint,
        "embed-resource" -> "content-link,related-content",
        "embed-id"       -> s"${articleId}"
      ) match {
        case Success(value) => value.results
        case Failure(_)     => Seq.empty
      }
    }

    def publishedWhereUsed(articleId: Long): Seq[SearchHit] = {
      get[SearchResults](
        SearchEndpointPublished,
        "embed-resource" -> "content-link,related-content",
        "embed-id"       -> s"${articleId}"
      ) match {
        case Success(value) => value.results
        case Failure(_)     => Seq.empty
      }
    }

    private def get[A](endpointUrl: String, params: (String, String)*)(implicit
        mf: Manifest[A],
        formats: org.json4s.Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(endpointUrl).method("GET").params(params.toMap))
    }
  }

}

case class SearchResults(totalCount: Int, results: Seq[SearchHit])
case class SearchHit(id: Long, title: Title)
case class Title(title: String, language: String)
