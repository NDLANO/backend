/*
 * Part of NDLA network
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import sttp.client3.Response

case class HttpRequestException(message: String, httpResponse: Response[String]) extends RuntimeException(message) {
  val code: Int      = httpResponse.code.code
  val is404: Boolean = httpResponse.code.code == 404
  val is409: Boolean = httpResponse.code.code == 409
  val is410: Boolean = httpResponse.code.code == 410
}
