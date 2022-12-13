/*
 * Part of NDLA article-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.integration

import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import no.ndla.articleapi.Props
import no.ndla.articleapi.service.ConverterService
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.{ArticleType, Availability}
import no.ndla.network.NdlaClient
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization.write
import scalaj.http.Http

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient with ConverterService with Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient(SearchApiBaseUrl: String = s"http://${props.SearchHost}") extends StrictLogging {

    private val InternalEndpoint = s"$SearchApiBaseUrl/intern"
    private val indexTimeout     = 1000 * 30

    def indexArticle(article: Article): Article = {
      implicit val formats: Formats =
        org.json4s.DefaultFormats +
          Json4s.serializer(ArticleType) +
          new EnumNameSerializer(Availability) ++
          JavaTimeSerializers.all

      implicit val executionContext: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)

      val future = postWithData[Article, Article](s"$InternalEndpoint/article/", article)
      future.onComplete {
        case Success(Success(_)) =>
          logger.info(s"Successfully indexed article with id: '${article.id
              .getOrElse(-1)}' and revision '${article.revision.getOrElse(-1)}' in search-api")
        case Failure(e) =>
          logger.error(
            s"Failed to indexed article with id: '${article.id
                .getOrElse(-1)}' and revision '${article.revision.getOrElse(-1)}' in search-api",
            e
          )
        case Success(Failure(e)) =>
          logger.error(
            s"Failed to indexed article with id: '${article.id
                .getOrElse(-1)}' and revision '${article.revision.getOrElse(-1)}' in search-api",
            e
          )
      }

      article
    }

    private def postWithData[A, B <: AnyRef](endpointUrl: String, data: B, params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats,
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

    def deleteArticle(id: Long): Long = {
      ndlaClient.fetchRawWithForwardedAuth(
        Http(s"$InternalEndpoint/article/$id")
          .timeout(indexTimeout, indexTimeout)
          .method("DELETE")
      )

      id
    }
  }

}
