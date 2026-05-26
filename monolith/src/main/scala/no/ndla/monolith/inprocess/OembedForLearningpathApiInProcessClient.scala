/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.learningpathapi.integration.OembedProxyClient
import no.ndla.learningpathapi.model.domain.InvalidOembedResponse
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

/** In-process implementation of learningpath-api's [[OembedProxyClient]] trait that delegates to oembed-proxy's
  * [[no.ndla.oembedproxy.service.OEmbedService]] directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.oembedproxy.ComponentRegistry]] and [[no.ndla.learningpathapi.ComponentRegistry]] in the monolith.
  *
  * Iframe-src extraction mirrors [[no.ndla.learningpathapi.integration.OembedProxyHttpClient]] — the HTTP client parses
  * the same html payload, so keeping the logic identical here preserves caller-visible behaviour.
  */
class OembedForLearningpathApiInProcessClient(producerCr: => no.ndla.oembedproxy.ComponentRegistry)
    extends OembedProxyClient {

  override def getIframeUrl(url: String): Try[String] = {
    producerCr.oEmbedService.get(url, None, None) match {
      case Failure(ex)     => Failure(ex)
      case Success(oembed) =>
        val html = oembed.html.getOrElse("")
        val soup = Jsoup.parse(html)
        val src  = Option(soup.selectFirst("iframe")).map(_.attr("src")).filterNot(_.isEmpty)
        src match {
          case Some(s) => Success(s)
          case None    => Failure(InvalidOembedResponse(s"Could not parse url in html from oembed-response for '$url'"))
        }
    }
  }
}
