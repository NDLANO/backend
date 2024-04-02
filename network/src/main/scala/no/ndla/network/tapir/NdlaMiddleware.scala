/*
 * Part of NDLA network
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.effect.IO
import no.ndla.common.RequestLogger.{afterRequestLogString, beforeRequestLogString}
import no.ndla.network.model.RequestInfo
import org.http4s.{Request, Response}
import org.log4s.{MDC, getLogger}

trait NdlaMiddleware {
  object NdlaMiddleware {
    private val logger = getLogger

    private def shouldLogRequest(req: Request[IO]): Boolean = {
      req.uri.path.renderString != "/health"
    }

    private def before(req: Request[IO]): Long = {
      val beforeTime = System.currentTimeMillis()

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

    private def after(
        beforeTime: Long,
        req: Request[IO],
        requestInfo: RequestInfo,
        resp: Response[IO]
    ): Response[IO] = {
      if (shouldLogRequest(req)) {
        MDC.put("correlationID", requestInfo.correlationId.get): Unit
        val latency = System.currentTimeMillis() - beforeTime
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

      resp
    }

    def apply(req: Request[IO], responseIO: IO[Response[IO]]): IO[Response[IO]] = {
      val reqInfo = RequestInfo.fromRequest(req)
      val set     = RequestInfo.set(reqInfo)
      MDC.put("correlationID", reqInfo.correlationId.get): Unit
      val beforeTime = before(req)

      set >> responseIO.map(resp => after(beforeTime, req, reqInfo, resp))
    }
  }
}
