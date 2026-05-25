/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.util.Try

case class OembedResponse(html: String)
object OembedResponse {
  implicit val encoder: Encoder[OembedResponse] = deriveEncoder
  implicit val decoder: Decoder[OembedResponse] = deriveDecoder
}

trait OembedProxyClient {
  def getIframeUrl(url: String): Try[String]
}
