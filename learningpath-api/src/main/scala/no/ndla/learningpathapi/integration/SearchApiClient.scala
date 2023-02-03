/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration
import java.util.concurrent.Executors
import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import no.ndla.common.model.domain.learningpath.EmbedType
import no.ndla.network.NdlaClient
import sttp.client3.quick._
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.domain._
import no.ndla.network.model.NdlaRequest
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write
import sttp.client3.Response

import scala.annotation.unused
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient with Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient extends StrictLogging {
    import props.SearchApiHost
    private val IndexTimeout = 90.seconds
    @unused
    private val SearchApiBaseUrl = s"http://$SearchApiHost"
    implicit val formats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        Json4s.serializer(StepStatus) +
        new EnumNameSerializer(EmbedType)

    def deleteLearningPathDocument(id: Long): Try[_] = {
      val req = quickRequest
        .delete(uri"http://$SearchApiHost/intern/learningpath/$id")
        .readTimeout(IndexTimeout)

      doRawRequest(req)
    }

    def indexLearningPathDocument(document: LearningPath): Future[Try[_]] = {
      val idString    = document.id.map(_.toString).getOrElse("<missing id>")
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
      val future = Future {
        val body = write(document)

        val req = quickRequest
          .post(uri"http://$SearchApiHost/intern/learningpath/")
          .header("Content-Type", "application/json", replaceExisting = true)
          .body(body)
          .readTimeout(IndexTimeout)

        doRawRequest(req)
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

    private def doRawRequest(request: NdlaRequest): Try[Response[String]] = {
      ndlaClient.fetchRawWithForwardedAuth(request) match {
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
