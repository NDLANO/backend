/*
 * Part of NDLA backend.network.main
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.http

import io.circe.Encoder

import scala.util.Try

sealed trait BaseReq {
  val method: String
  val url: String
  val headers: Map[String, String]

  def send(): Try[Response]
}

case class Req(
    method: String,
    url: String,
    headers: Map[String, String],
    body: Option[String]
) extends BaseReq {
  override def send(): Try[Response] = HttpClient.send(this)
}

case class JsonReq[T: Encoder](
    method: String,
    url: String,
    headers: Map[String, String],
    body: T
) extends BaseReq {
  override def send(): Try[Response] = HttpClient.send(this)
}
