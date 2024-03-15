/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class OEmbedEndpoint(
    schemes: Option[List[String]],
    url: Option[String],
    discovery: Option[Boolean],
    formats: Option[List[String]],
    mandatoryQueryParams: List[(String, String)] = List()
) {

  def supports(url: String): Boolean = {
    schemes match {
      case None => false
      case Some(schemesList) =>
        schemesList.exists(scheme => matches(scheme, url))
    }
  }

  def matches(scheme: String, url: String): Boolean = {
    val regex = scheme.replace(".", "\\.").replace("*", ".*")
    url.matches(regex)
  }
}

object OEmbedEndpoint {
  implicit val encoder: Encoder[OEmbedEndpoint] = deriveEncoder
  implicit val decoder: Decoder[OEmbedEndpoint] = deriveDecoder
}
