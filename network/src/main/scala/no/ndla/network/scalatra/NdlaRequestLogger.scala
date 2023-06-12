/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.scalatra

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.RequestLogger.afterRequestLogString
import no.ndla.network.model.RequestInfo
import org.eclipse.jetty.server.{Request, RequestLog, Response}

class NdlaRequestLogger() extends RequestLog with StrictLogging {

  override def log(request: Request, response: Response): Unit = {
    if (request.getRequestURI == "/health") return // Logging health-endpoints are very noisy

    val latency = System.currentTimeMillis() - request.getTimeStamp
    val query   = Option(request.getQueryString).getOrElse("")
    logger.info(
      afterRequestLogString(
        method = request.getMethod,
        requestPath = request.getRequestURI,
        queryString = query,
        latency = latency,
        responseCode = response.getStatus
      )
    )

    RequestInfo.clear()
  }
}
