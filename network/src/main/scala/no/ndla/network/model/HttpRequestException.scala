/*
 * Part of NDLA network
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import sttp.client3.Response

class HttpRequestException(message: String, val httpResponse: Option[Response[String]] = None)
    extends RuntimeException(message) {
  def code: Int      = httpResponse.map(_.code.code).getOrElse(-1)
  def is404: Boolean = httpResponse.exists(_.code.code == 404)
  def is409: Boolean = httpResponse.exists(_.code.code == 409)
  def is410: Boolean = httpResponse.exists(_.code.code == 410)
}

class AuthorizationException(message: String) extends RuntimeException(message)
