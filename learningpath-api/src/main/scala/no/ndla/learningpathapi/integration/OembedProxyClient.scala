/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.domain.*
import no.ndla.network.NdlaClient
import org.jsoup.Jsoup
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

case class OembedResponse(html: String)
object OembedResponse {
  implicit val encoder: Encoder[OembedResponse] = deriveEncoder
  implicit val decoder: Decoder[OembedResponse] = deriveDecoder
}

trait OembedProxyClient {
  this: NdlaClient & Props =>
  val oembedProxyClient: OembedProxyClient

  class OembedProxyClient extends StrictLogging {
    import props.ApiGatewayHost
    private val OembedProxyTimeout = 90.seconds
    private val OembedProxyBaseUrl = s"http://$ApiGatewayHost/oembed-proxy/v1"

    def getIframeUrl(url: String): Try[String] = {
      getOembed(url) match {
        case Failure(ex)     => Failure(ex)
        case Success(oembed) =>
          val soup = Jsoup.parse(oembed.html)
          val elem = Option(soup.selectFirst("iframe"))
          Option(elem.map(_.attr("src")).filterNot(_.isEmpty)).flatten match {
            case Some(url) => Success(url)
            case None => Failure(InvalidOembedResponse(s"Could not parse url in html from oembed-response for '$url'"))
          }
      }
    }

    private def getOembed(url: String): Try[OembedResponse] = {
      get[OembedResponse](s"$OembedProxyBaseUrl/oembed", "url" -> url)
    }

    private def get[A: Decoder](url: String, params: (String, String)*): Try[A] = {
      val request = quickRequest.get(uri"$url".withParams(params.toMap)).readTimeout(OembedProxyTimeout)
      ndlaClient.fetchWithForwardedAuth[A](request, None)
    }
  }

}
