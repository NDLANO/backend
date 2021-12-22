/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

case class OEmbedEndpoint(schemes: Option[List[String]],
                          url: Option[String],
                          discovery: Option[Boolean],
                          formats: Option[List[String]],
                          mandatoryQueryParams: List[(String, String)] = List()) {

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
