/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import java.util.concurrent.{Executor, Executors}

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.dsl._
import no.ndla.draftapi.DraftApiProperties.H5PAddress
import no.ndla.draftapi.model.api.H5PException
import no.ndla.network.NdlaClient
import org.json4s.DefaultFormats
import scalaj.http.Http

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import cats.implicits._
import no.ndla.draftapi.model.domain.RequestInfo

trait H5PApiClient {
  this: NdlaClient =>
  val h5pApiClient: H5PApiClient

  class H5PApiClient extends LazyLogging {
    private val H5PApi = s"$H5PAddress/v1"
    private val h5pTimeout = 20 * 1000 // 20 Seconds
    implicit val formats: DefaultFormats.type = DefaultFormats

    def publishH5Ps(paths: Seq[String]): Try[Unit] = {
      if (paths.isEmpty) {
        Success(())
      } else {
        implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(paths.size))
        val future = Future.sequence(paths.map(publishH5P))
        Try(Await.result(future, Duration.Inf)) match {
          case Failure(ex) => Failure(ex)
          case Success(s)  => s.toList.sequence.map(_ => ())
        }
      }
    }

    private def publishH5P(path: String)(implicit ec: ExecutionContext): Future[Try[Unit]] = {
      path.path.parts.lastOption match {
        case None =>
          Future.successful {
            val msg = "Got h5p path without id. Not publishing..."
            logger.error(msg)
            Failure(H5PException(msg))
          }
        case Some(h5pId) =>
          val future = putNothing(s"$H5PApi/resource/$h5pId/publish")
          logWhenComplete(future, path, h5pId)
          future
      }
    }

    private def logWhenComplete(future: Future[Try[Unit]], path: String, h5pId: String)(
        implicit ec: ExecutionContext) = {
      future.onComplete {
        case Failure(ex) =>
          logger.error(s"failed to publish h5p with path '$path' (id '$h5pId'): ${ex.getMessage}", ex)
        case Success(t) =>
          t match {
            case Failure(ex) =>
              logger.error(s"failed to publish h5p with path '$path' (id '$h5pId'): ${ex.getMessage}", ex)
              Failure(ex)
            case Success(res) =>
              logger.info(s"Successfully published (or republished) h5p with path '$path' (id '$h5pId')")
              Success(res)
          }
      }
    }

    private[integration] def putNothing(url: String, params: (String, String)*)(
        implicit formats: org.json4s.Formats,
        ec: ExecutionContext): Future[Try[Unit]] = {
      val threadInfo = RequestInfo()
      Future {
        logger.info(s"Doing call to $url")
        threadInfo.setRequestInfo()
        ndlaClient.fetchRawWithForwardedAuth(
          Http(url)
            .method("PUT")
            .timeout(h5pTimeout, h5pTimeout)
            .header("content-type", "application/json")
            .params(params)
        ) match {
          case Success(_)  => Success(())
          case Failure(ex) => Failure(ex)
        }
      }
    }
  }
}
