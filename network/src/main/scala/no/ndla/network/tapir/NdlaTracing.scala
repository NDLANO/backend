/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import io.opentelemetry.api.trace.Span
import sttp.monad.MonadError
import sttp.shared.Identity
import sttp.tapir.server.interceptor.{
  DecodeFailureContext,
  DecodeSuccessContext,
  EndpointHandler,
  EndpointInterceptor,
  Responder,
  SecurityFailureContext,
}
import sttp.tapir.server.interpreter.BodyListener
import sttp.tapir.server.model.ServerResponse

/** OpenTelemetry helpers used to enrich the spans created by the OpenTelemetry Java agent. All calls are safe no-ops
  * when no agent/SDK is attached: in that case `Span.current()` returns an invalid, non-recording span.
  */
object NdlaTracing {

  /** Set an attribute on the currently active span, if there is a valid one. */
  def setSpanAttribute(key: String, value: String): Unit = {
    val span = Span.current()
    if (span.getSpanContext.isValid) {
      span.setAttribute(key, value): Unit
    }
  }

  /** Endpoint interceptor that renames the active span after the matched endpoint (e.g. `GET /v1/articles/{id}`) and
    * sets the `http.route` attribute. The agent instruments raw Netty, which has no knowledge of Tapir routes, so
    * without this every server span would be named `/`.
    */
  val spanNamingInterceptor: EndpointInterceptor[Identity] = new EndpointInterceptor[Identity] {
    override def apply[B](
        responder: Responder[Identity, B],
        endpointHandler: EndpointHandler[Identity, B],
    ): EndpointHandler[Identity, B] = new EndpointHandler[Identity, B] {

      override def onDecodeSuccess[A, U, I](
          ctx: DecodeSuccessContext[Identity, A, U, I]
      )(implicit monad: MonadError[Identity], bodyListener: BodyListener[Identity, B]): Identity[ServerResponse[B]] = {
        nameSpanFromEndpoint(ctx.request.method.method, ctx.endpoint.showPathTemplate(showQueryParam = None))
        endpointHandler.onDecodeSuccess(ctx)
      }

      override def onSecurityFailure[A](
          ctx: SecurityFailureContext[Identity, A]
      )(implicit monad: MonadError[Identity], bodyListener: BodyListener[Identity, B]): Identity[ServerResponse[B]] =
        endpointHandler.onSecurityFailure(ctx)

      override def onDecodeFailure(ctx: DecodeFailureContext)(implicit
          monad: MonadError[Identity],
          bodyListener: BodyListener[Identity, B],
      ): Identity[Option[ServerResponse[B]]] = endpointHandler.onDecodeFailure(ctx)
    }
  }

  private def nameSpanFromEndpoint(method: String, pathTemplate: String): Unit = {
    val span = Span.current()
    if (span.getSpanContext.isValid) {
      span.updateName(s"$method $pathTemplate")
      span.setAttribute("http.route", pathTemplate): Unit
    }
  }
}
