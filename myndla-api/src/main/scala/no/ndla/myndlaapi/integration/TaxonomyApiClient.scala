/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.myndlaapi.Props
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.util.{Success, Try}

class TaxonomyApiClient(using ndlaClient: NdlaClient, props: Props) extends StrictLogging {
  private val resolveEndpoint = s"${props.TaxonomyUrl}/v1/url/resolve"

  def resolveUrl(path: String): Try[String] = {
    val req = quickRequest.get(uri"$resolveEndpoint?path=$path")
    ndlaClient.fetch[ResolvePathResponse](req).map(resolved => resolved.url).orElse(Success(path))
  }
}

case class ResolvePathResponse(url: String)
object ResolvePathResponse {
  implicit def decoder: Decoder[ResolvePathResponse] = deriveDecoder
  implicit def encoder: Encoder[ResolvePathResponse] = deriveEncoder
}
