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
    getScheme: String,
    getHeaderFunc: String => Option[String],
    serverName: String,
    servletPath: String
) {
  def getHeader(name: String): Option[String] = getHeaderFunc(name)
}

object NdlaHttpRequest {

  def apply(req: HttpServletRequest): NdlaHttpRequest =
    NdlaHttpRequest(
      serverPort = req.getServerPort,
      getHeaderFunc = name => Option(req.getHeader(name)),
      getScheme = req.getScheme,
      serverName = req.getServerName,
      servletPath = req.getServletPath
    )
}
