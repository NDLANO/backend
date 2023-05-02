/*
 * Part of NDLA network.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.effect.IO
import no.ndla.common.CorrelationID
import no.ndla.common.RequestLogger.{afterRequestLogString, beforeRequestLogString}
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.NdlaHttpRequest
import org.http4s.{Request, Response}
import org.log4s.getLogger
import org.typelevel.ci.CIString

trait NdlaMiddleware {
  this: Service =>
  object NdlaMiddleware {
    private val CorrelationIdHeader = CIString("X-Correlation-ID")
    private val logger              = getLogger

    def asNdlaHttpRequest[F[+_]](req: Request[F]): NdlaHttpRequest =
      NdlaHttpRequest(
        serverPort = req.serverPort.map(_.value).getOrElse(-1),
        getHeaderFunc = name => req.headers.get(CIString(name)).map(_.head.value),
        getScheme = req.uri.scheme.map(_.value).getOrElse("http"),
        serverName = req.serverAddr.map(_.toUriString).getOrElse("localhost"),
        servletPath = req.uri.path.renderString
      )

    private def shouldLogRequest(req: Request[IO]): Boolean = {
      req.uri.path.renderString != "/health"
    }

    private def before(req: Request[IO]): Long = {
      val beforeTime = System.currentTimeMillis()

      val correlationIdHeader = req.headers.get(CorrelationIdHeader).map(_.head.value)
      CorrelationID.set(correlationIdHeader)
      ApplicationUrl.set(asNdlaHttpRequest(req))
      if (shouldLogRequest(req)) {
        logger.info(
          beforeRequestLogString(
            method = req.method.name,
            requestPath = req.uri.path.renderString,
            queryString = req.queryString
          )
        )
      }

      beforeTime
    }

    private def after(beforeTime: Long, req: Request[IO], resp: Response[IO]): Response[IO] = {
      val latency = System.currentTimeMillis() - beforeTime

      if (shouldLogRequest(req)) {
        logger.info(
          afterRequestLogString(
            method = req.method.name,
            requestPath = req.uri.path.renderString,
            queryString = req.queryString,
            latency = latency,
            responseCode = resp.status.code
          )
        )
      }

      CorrelationID.clear()
      ApplicationUrl.clear()

      resp
    }

    def apply(req: Request[IO], responseIO: IO[Response[IO]]): IO[Response[IO]] = {
      val beforeTime = before(req)
      responseIO.map(resp => after(beforeTime, req, resp))
    }
  }
}
