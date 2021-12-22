/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.{Effect, IO}
import no.ndla.network.model.NdlaHttpRequest
import no.ndla.network.{ApplicationUrl, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpRoutes, Request, Response}
import org.log4s.getLogger

object NdlaMiddleware {
  private val CorrelationIdHeader = CaseInsensitiveString("X-Correlation-ID")
  private val CorrelationIdKey = "correlationID"
  private val logger = getLogger

  def asNdlaHttpRequest[F[+ _]: Effect](req: Request[F]): NdlaHttpRequest = {
    new NdlaHttpRequest {
      override def serverPort: Int = req.serverPort
      override def getHeader(name: String): Option[String] = req.headers.get(CaseInsensitiveString(name)).map(_.value)
      override def getScheme: String = req.uri.scheme.map(_.value).getOrElse("http")
      override def serverName: String = req.serverAddr
      override def servletPath: String = req.uri.path
    }
  }

  private def before(service: HttpRoutes[IO]): HttpRoutes[IO] = cats.data.Kleisli { req: Request[IO] =>
    CorrelationID.set(req.headers.get(CorrelationIdHeader).map(_.value))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    ApplicationUrl.set(asNdlaHttpRequest(req))
    logger.info(s"${req.method} ${req.uri}${req.queryString}")
    service(req)
  }

  private def after(resp: Response[IO]): Response[IO] = {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    ApplicationUrl.clear()

    resp
  }

  def apply(service: HttpRoutes[IO]): HttpRoutes[IO] = {
    before(service).map(after)
  }
}
