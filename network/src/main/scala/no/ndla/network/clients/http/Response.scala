/*
 * Part of NDLA backend.network.main
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.http

import io.circe.Decoder
import no.ndla.common.CirceUtil

import scala.util.Try

case class Response(body: String, statusCode: Int, responseHeaders: Map[String, Seq[String]]) {
  def header(name: String): Option[String] = responseHeaders.get(name).flatMap(_.headOption)
  def bodyAs[T: Decoder]: Try[T]           = CirceUtil.tryParseAs[T](body)
}
