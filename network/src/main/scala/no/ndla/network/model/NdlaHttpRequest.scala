/*
 * Part of NDLA network.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import javax.servlet.http.HttpServletRequest

case class NdlaHttpRequest(
    serverPort: Int,
    getHeader: String => Option[String],
    getScheme: String,
    serverName: String,
    servletPath: String
)

object NdlaHttpRequest {

  def apply(req: HttpServletRequest): NdlaHttpRequest =
    NdlaHttpRequest(
      serverPort = req.getServerPort,
      getHeader = name => Option(req.getHeader(name)),
      getScheme = req.getScheme,
      serverName = req.getServerName,
      servletPath = req.getServletPath
    )
}
