/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import cats.implicits.*
import io.circe.generic.auto.*
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.oembedproxy.model.*
import no.ndla.oembedproxy.service.OEmbedServiceComponent
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait OEmbedProxyController {
  this: OEmbedServiceComponent & ErrorHandling & TapirController =>
  lazy val oEmbedProxyController: OEmbedProxyController

  class OEmbedProxyController extends TapirController {
    override val serviceName: String                       = "oembed"
    override val prefix: EndpointInput[Unit]               = "oembed-proxy" / "v1" / serviceName
    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      endpoint.get
        .summary("Returns oEmbed information for a given url.")
        .description("Returns oEmbed information for a given url.")
        .in(query[String]("url").description("The URL to retrieve embedding information for"))
        .in(query[Option[String]]("maxwidth").description("The maximum width of the embedded resource"))
        .in(query[Option[String]]("maxheight").description("The maximum height of the embedded resource"))
        .errorOut(errorOutputsFor(400, 401, 403, 404, 410, 422, 502))
        .out(jsonBody[OEmbedDTO])
        .serverLogicPure { case (url, maxWidth, maxHeight) =>
          println("jeg bor her")
          oEmbedService.get(url, maxWidth, maxHeight) match {
            case Success(oembed) => oembed.asRight
            case Failure(ex)     => returnLeftError(ex)
          }
        }
    )
  }
}
