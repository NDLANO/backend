/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import javax.servlet.http.HttpServletRequest
import no.ndla.network.model.NdlaHttpRequest
import scala.util.Properties.propOrNone

object ApplicationUrl {
  val X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto"
  val X_FORWARDED_HOST_HEADER  = "X-Forwarded-Host"
  val FORWARDED_HEADER         = "Forwarded"
  val FORWARDED_PROTO          = "proto"
  val HTTP                     = "http"
  val HTTPS                    = "https"
  val HTTP_PORT                = 80
  val HTTPS_PORT               = 443

  val applicationUrl = new ThreadLocal[String]

  def set(request: HttpServletRequest): Unit = set(NdlaHttpRequest(request))
  def set(str: String): Unit                 = applicationUrl.set(str)
  def set(request: NdlaHttpRequest): Unit    = set(fromRequest(request))

  def fromRequest(request: HttpServletRequest): String = fromRequest(NdlaHttpRequest(request))

  def fromRequest(request: NdlaHttpRequest): String = {
    propOrNone("NDLA_ENVIRONMENT") match {
      case Some(environment) if environment.nonEmpty => s"${Domains.get(environment)}${request.servletPath}/"
      case _ =>
        val xForwardedProtoHeaderProtocol = request.getHeader(X_FORWARDED_PROTO_HEADER)
        val forwardedHeaderProtocol = request
          .getHeader(FORWARDED_HEADER)
          .flatMap(
            _.replaceAll("\\s", "").split(";").find(_.contains(FORWARDED_PROTO)).map(_.dropWhile(c => c != '=').tail)
          )
        val schemeProtocol =
          if (request.serverPort == HTTP_PORT || request.serverPort == HTTPS_PORT) Some(request.getScheme) else None

        val host = request.getHeader(X_FORWARDED_HOST_HEADER).getOrElse(request.serverName)

        List(forwardedHeaderProtocol, xForwardedProtoHeaderProtocol, schemeProtocol).collectFirst {
          case Some(protocol) if protocol == HTTP || protocol == HTTPS => protocol
        } match {
          case Some(protocol) => s"$protocol://$host${request.servletPath}/"
          case None           => s"${request.getScheme}://$host:${request.serverPort}${request.servletPath}/"
        }
    }
  }

  def get: String   = applicationUrl.get
  def clear(): Unit = applicationUrl.remove()
}
