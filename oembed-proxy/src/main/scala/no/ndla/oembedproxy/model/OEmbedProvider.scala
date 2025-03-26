/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import io.circe.Decoder
import io.lemonlabs.uri.Url
import io.lemonlabs.uri.typesafe.dsl.*

case class OEmbedProvider(
    providerName: String,
    providerUrl: String,
    endpoints: List[OEmbedEndpoint],
    urlParser: String => String = identity,
    postProcessor: (String, OEmbedDTO) => OEmbedDTO = (_: String, o: OEmbedDTO) => o
) {

  def supports(url: String): Boolean = {
    endpoints.exists(_.supports(url)) || hostMatches(url)
  }

  def hostMatches(url: String): Boolean = {
    val urlHost      = url.hostOption.filter(_.toString != "")
    val providerHost = providerUrl.hostOption.filter(_.toString != "")
    urlHost.exists(providerHost.contains)
  }

  private def _requestUrl(url: String, maxWidth: Option[String], maxHeight: Option[String]): String = {
    endpoints.collectFirst { case e if e.supports(url) && e.url.nonEmpty => e } match {
      case None =>
        val validUrls = endpoints.map(_.url)
        throw ProviderNotSupportedException(
          s"The provider '$providerName' does not support the provided url '$url'. Must be one of [${validUrls.mkString(",")}]"
        )
      case Some(endpoint) =>
        val embedUrl = endpoint.url.replace("{format}", "json") // Some providers have {format} instead of ?format=
        val width    = maxWidth.map(("maxwidth", _)).toList
        val height   = maxHeight.map(("maxheight", _)).toList
        val params = List(("url", url), ("format", "json")) ++ endpoint.mandatoryQueryParams.getOrElse(
          List.empty
        ) ++ width ++ height
        Url.parse(embedUrl).addParams(params).toString
    }
  }

  def requestUrl(url: String, maxWidth: Option[String], maxHeight: Option[String]): String =
    _requestUrl(urlParser(url), maxWidth, maxHeight)
}

object OEmbedProvider {
  implicit val decoder: Decoder[OEmbedProvider] = Decoder.instance { cur =>
    for {
      providerName <- cur.downField("provider_name").as[String]
      providerUrl  <- cur.downField("provider_url").as[String]
      endpoints    <- cur.downField("endpoints").as[List[OEmbedEndpoint]]
    } yield OEmbedProvider(providerName, providerUrl, endpoints)

  }
}
