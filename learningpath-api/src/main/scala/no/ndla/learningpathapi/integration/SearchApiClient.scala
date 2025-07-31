/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration
import java.util.concurrent.Executors
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.network.NdlaClient
import sttp.client3.quick.*
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.domain.*
import no.ndla.network.model.NdlaRequest
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.Response

import scala.annotation.unused
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient & Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient extends StrictLogging {
    import props.SearchApiHost
    private val IndexTimeout = 90.seconds
    @unused
    private val SearchApiBaseUrl = s"http://$SearchApiHost"

    def deleteLearningPathDocument(id: Long, user: Option[TokenUser]): Try[?] = {
      val req = quickRequest
        .delete(uri"http://$SearchApiHost/intern/learningpath/$id")
        .readTimeout(IndexTimeout)

      doRawRequest(req, user)
    }

    def indexLearningPathDocument(document: LearningPath, user: Option[TokenUser]): Future[Try[?]] = {
      val idString                              = document.id.map(_.toString).getOrElse("<missing id>")
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
      val future                                = Future {
        val body = CirceUtil.toJsonString(document)

        val req = quickRequest
          .post(uri"http://$SearchApiHost/intern/learningpath/")
          .header("Content-Type", "application/json", replaceExisting = true)
          .body(body)
          .readTimeout(IndexTimeout)

        doRawRequest(req, user)
      }

      future.onComplete {
        case Success(req) =>
          req match {
            case Failure(ex) =>
              logger.error(s"Failed when calling search-api for indexing '$idString': '${ex.getMessage}'", ex)
            case Success(response) if !response.isSuccess =>
              logger.error(
                s"Failed when calling search-api for indexing '$idString': '${response.code}' -> '${response.body}'"
              )
            case Success(_) =>
              logger.info(s"Successfully called search-api for indexing '$idString'")
          }
        case Failure(ex) =>
          logger.error(s"Future failed when calling search-api for indexing '$idString': '${ex.getMessage}'", ex)
      }

      future
    }

    private def doRawRequest(request: NdlaRequest, user: Option[TokenUser]): Try[Response[String]] = {
      ndlaClient.fetchRawWithForwardedAuth(request, user) match {
        case Success(r) =>
          if (r.code.isSuccess)
            Success(r)
          else
            Failure(
              SearchException(
                s"Got status code '${r.code}' when attempting to request search-api. Body was: '${r.body}'"
              )
            )
        case Failure(ex) => Failure(ex)

      }
    }

  }

}
