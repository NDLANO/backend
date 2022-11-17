/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.{Effect, IO}
import no.ndla.common.CorrelationID
import no.ndla.common.RequestLogger.{afterRequestLogString, beforeRequestLogString}
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.NdlaHttpRequest
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpRoutes, Request, Response}
import org.log4s.getLogger

object NdlaMiddleware {
  private val CorrelationIdHeader = CaseInsensitiveString("X-Correlation-ID")
  private val logger              = getLogger

  def asNdlaHttpRequest[F[+_]: Effect](req: Request[F]): NdlaHttpRequest = {
    new NdlaHttpRequest {
      override def serverPort: Int                         = req.serverPort
      override def getHeader(name: String): Option[String] = req.headers.get(CaseInsensitiveString(name)).map(_.value)
      override def getScheme: String                       = req.uri.scheme.map(_.value).getOrElse("http")
      override def serverName: String                      = req.serverAddr
      override def servletPath: String                     = req.uri.path
    }
  }

  private def before(service: HttpRoutes[IO]): HttpRoutes[IO] = cats.data.Kleisli { req: Request[IO] =>
    val beforeTime = System.currentTimeMillis()

    CorrelationID.set(req.headers.get(CorrelationIdHeader).map(_.value))
    ApplicationUrl.set(asNdlaHttpRequest(req))
    logger.info(
      beforeRequestLogString(
        method = req.method.name,
        requestPath = req.uri.path,
        queryString = req.queryString
      )
    )

    service(req).map { resp =>
      after(beforeTime, req, resp)
    }
  }

  private def after(beforeTime: Long, req: Request[IO], resp: Response[IO]): Response[IO] = {
    CorrelationID.clear()
    ApplicationUrl.clear()

    val latency = System.currentTimeMillis() - beforeTime

    logger.info(
      afterRequestLogString(
        method = req.method.name,
        requestPath = req.uri.path,
        queryString = req.queryString,
        latency = latency,
        responseCode = resp.status.code
      )
    )

    resp
  }

  def apply(service: HttpRoutes[IO]): HttpRoutes[IO] = {
    before(service)
  }
}
