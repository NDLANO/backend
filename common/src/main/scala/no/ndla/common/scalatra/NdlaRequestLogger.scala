/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.scalatra

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CorrelationID
import no.ndla.common.configuration.BaseProps
import org.eclipse.jetty.server.{Request, RequestLog, Response}

class NdlaRequestLogger[PROPS <: BaseProps](props: PROPS) extends RequestLog with StrictLogging {

  override def log(request: Request, response: Response): Unit = {
    if (request.getRequestURI == "/health") return // Logging health-endpoints are very noisy

    val latency = System.currentTimeMillis() - request.getTimeStamp
    val query   = Option(request.getQueryString).map(s => s"?$s").getOrElse("")
    logger.info(
      s"${request.getMethod} ${request.getRequestURI}${query} executed in ${latency}ms with code ${response.getStatus}"
    )

    CorrelationID.clear()
  }
}
