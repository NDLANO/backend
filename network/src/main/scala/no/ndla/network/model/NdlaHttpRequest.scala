/*
 * Part of NDLA network.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import javax.servlet.http.HttpServletRequest

trait NdlaHttpRequest {
  def serverPort: Int
  def getHeader(name: String): Option[String]
  def getScheme: String
  def serverName: String
  def servletPath: String
}

object NdlaHttpRequest {

  def apply(req: HttpServletRequest): NdlaHttpRequest =
    new NdlaHttpRequest {
      override def serverPort: Int = req.getServerPort
      override def getHeader(name: String): Option[String] = Option(req.getHeader(name))
      override def getScheme: String = req.getScheme
      override def serverName: String = req.getServerName
      override def servletPath: String = req.getServletPath
    }
}
