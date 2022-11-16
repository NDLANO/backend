/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.scalatra

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.{Request, RequestLog, Response}

class NdlaRequestLogger extends RequestLog with LazyLogging {
  override def log(request: Request, response: Response): Unit = {
    if (request.getRequestURI == "/health") return // Logging health-endpoints are very noisy

    val latency = System.currentTimeMillis() - request.getTimeStamp
    val query   = Option(request.getQueryString).map(s => s"?$s").getOrElse("")
    logger.info(
      s"${request.getMethod} ${request.getRequestURI}${query} executed in ${latency}ms with code ${response.getStatus}"
    )
  }
}
