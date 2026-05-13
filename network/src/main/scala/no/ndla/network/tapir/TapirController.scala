/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.common.SchemaImplicits
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import sttp.model.StatusCode
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

abstract class TapirController(using errorHandling: ErrorHandling)
    extends TapirErrorHandling,
      StrictLogging,
      SchemaImplicits {
  type Eff[A] = Identity[A]
  val enableSwagger: Boolean = true
  val serviceName: String    = this.getClass.getSimpleName
  protected val prefix: EndpointInput[Unit]
  val endpoints: List[ServerEndpoint[Any, Eff]]

  lazy val builtEndpoints: List[ServerEndpoint[Any, Eff]] = {
    this
      .endpoints
      .map(e => {
        ServerEndpoint(
          endpoint = e.endpoint.prependIn(this.prefix).tag(this.serviceName),
          securityLogic = e.securityLogic,
          logic = e.logic,
        )
      })
  }

  /** Helper to simplify returning _both_ NoContent and some json body T from an endpoint */
  def noContentOrBodyOutput[T: {Encoder, Decoder, Schema}]: EndpointOutput.OneOf[Option[T], Option[T]] = {
    val noContentVariant = noContent.and(emptyOutputAs[Option[T]](None))
    val okVariant        = statusCode(StatusCode.Ok).and(jsonBody[Option[T]])
    oneOf[Option[T]](
      oneOfVariantValueMatcher(okVariant) { case Some(_) =>
        true
      },
      oneOfVariantValueMatcher(noContentVariant) { case None =>
        true
      },
    )
  }

  private val zeroNoContentHeader: EndpointIO.FixedHeader[Unit] = header("Content-Length", "0")

  // NOTE: We use our own emptyOutput to add the `Content-Length` header
  //       to signify no output body, since openapi-fetch doesn't react nicely
  //       200 OK responses without body and no `Content-Length` header.
  def emptyOutput: EndpointOutput[Unit] = sttp.tapir.emptyOutput.and(zeroNoContentHeader)
  def noContent: EndpointOutput[Unit]   = statusCode(StatusCode.NoContent)
}
