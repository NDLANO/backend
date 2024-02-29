/*
 * Part of NDLA network.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import org.http4s.Request
import org.typelevel.ci.CIString
import sttp.tapir.model.ServerRequest

case class NdlaHttpRequest(
    serverPort: Int,
    getScheme: String,
    getHeaderFunc: String => Option[String],
    serverName: String,
    servletPath: String
) {
  def getHeader(name: String): Option[String] = getHeaderFunc(name)
  def getToken: Option[String]                = getHeader("Authorization").map(_.replace("Bearer ", ""))
}

object NdlaHttpRequest {

  def from(req: ServerRequest): NdlaHttpRequest = {
    val port   = req.uri.port
    val scheme = req.uri.scheme
    NdlaHttpRequest(
      serverPort = port.getOrElse(-1),
      getHeaderFunc = name => req.header(name),
      getScheme = scheme.getOrElse("http"),
      serverName = req.uri.host.getOrElse("localhost"),
      servletPath = s"/${req.uri.path.mkString("/")}"
    )
  }

  def from[F[+_]](req: Request[F]): NdlaHttpRequest =
    NdlaHttpRequest(
      serverPort = req.serverPort.map(_.value).getOrElse(-1),
      getHeaderFunc = name => req.headers.get(CIString(name)).map(_.head.value),
      getScheme = req.uri.scheme.map(_.value).getOrElse("http"),
      serverName = req.serverAddr.map(_.toUriString).getOrElse("localhost"),
      servletPath = req.uri.path.renderString
    )

}
