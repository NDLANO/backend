/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.oembedproxy.model._
import no.ndla.oembedproxy.service.OEmbedServiceComponent
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait OEmbedProxyController {
  this: OEmbedServiceComponent with ErrorHelpers with Service =>
  val oEmbedProxyController: OEmbedProxyController

  class OEmbedProxyController extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "oembed-proxy" / "v1" / "oembed"
    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .summary("Returns oEmbed information for a given url.")
        .description("Returns oEmbed information for a given url.")
        .in(query[String]("url").description("The URL to retrieve embedding information for"))
        .in(query[Option[String]]("maxwidth").description("The maximum width of the embedded resource"))
        .in(query[Option[String]]("maxheight").description("The maximum height of the embedded resource"))
        .errorOut(errorOutputsFor(400, 404, 501, 502))
        .out(jsonBody[OEmbed])
        .serverLogicPure { case (url, maxWidth, maxHeight) =>
          oEmbedService.get(url, maxWidth, maxHeight) match {
            case Success(oembed) => oembed.asRight
            case Failure(ex)     => returnError(ex).asLeft
          }
        }
    )
  }
}
