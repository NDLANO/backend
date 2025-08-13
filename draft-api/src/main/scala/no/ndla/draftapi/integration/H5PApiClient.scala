/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.H5PException
import no.ndla.network.NdlaClient
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick._

import java.util.concurrent.Executors
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait H5PApiClient {
  this: NdlaClient with Props =>
  lazy val h5pApiClient: H5PApiClient

  class H5PApiClient extends StrictLogging {
    private val H5PApi     = s"${props.H5PAddress}/v1"
    private val h5pTimeout = 20.seconds

    def publishH5Ps(paths: Seq[String], user: TokenUser): Try[Unit] = {
      if (paths.isEmpty) {
        Success(())
      } else {
        implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(paths.size))
        val future      = Future.sequence(paths.map(publishH5P(_, user)))
        Try(Await.result(future, Duration.Inf)) match {
          case Failure(ex) => Failure(ex)
          case Success(s)  => s.toList.sequence.map(_ => ())
        }
      }
    }

    private def publishH5P(path: String, user: TokenUser)(implicit ec: ExecutionContext): Future[Try[Unit]] = {
      path.path.parts.lastOption match {
        case None =>
          Future.successful {
            val msg = "Got h5p path without id. Not publishing..."
            logger.error(msg)
            Failure(H5PException(msg))
          }
        case Some(h5pId) =>
          val future = putNothing(s"$H5PApi/resource/$h5pId/publish", user)
          logWhenComplete(future, path, h5pId)
          future
      }
    }

    private def logWhenComplete(future: Future[Try[Unit]], path: String, h5pId: String)(implicit
        ec: ExecutionContext
    ) = {
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

    private[integration] def putNothing(url: String, user: TokenUser, params: (String, String)*)(implicit
        ec: ExecutionContext
    ): Future[Try[Unit]] = {
      val threadInfo = RequestInfo.fromThreadContext()
      Future {
        logger.info(s"Doing call to $url")
        threadInfo.setThreadContextRequestInfo()
        ndlaClient.fetchRawWithForwardedAuth(
          quickRequest
            .put(uri"$url".withParams(params: _*))
            .header("content-type", "application/json")
            .readTimeout(h5pTimeout),
          Some(user)
        ) match {
          case Success(_)  => Success(())
          case Failure(ex) => Failure(ex)
        }
      }
    }
  }
}
