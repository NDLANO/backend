/*
 * Part of NDLA article-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.articleapi.Props
import no.ndla.articleapi.service.ConverterService
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Article
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient & ConverterService & Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient(SearchApiBaseUrl: String = props.SearchApiUrl) extends StrictLogging {

    private val InternalEndpoint = s"$SearchApiBaseUrl/intern"
    private val indexTimeout     = 30.seconds

    def indexArticle(article: Article): Article = {
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

    private def postWithData[A: Decoder, B <: AnyRef: Encoder](endpointUrl: String, data: B, params: (String, String)*)(
        implicit executionContext: ExecutionContext
    ): Future[Try[A]] = {
      Future {
        ndlaClient.fetch[A](
          quickRequest
            .post(uri"$endpointUrl".withParams(params.toMap))
            .body(CirceUtil.toJsonString(data))
            .readTimeout(indexTimeout)
            .header("content-type", "application/json", replaceExisting = true)
        )
      }
    }

    def deleteArticle(id: Long): Long = {
      ndlaClient.doRequest(
        quickRequest
          .delete(uri"$InternalEndpoint/article/$id")
          .readTimeout(indexTimeout)
      ): Unit

      id
    }
  }

}
