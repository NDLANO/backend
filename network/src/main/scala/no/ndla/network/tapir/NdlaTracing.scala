/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.{Span, SpanKind, StatusCode}
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import no.ndla.common.CorrelationID
import sttp.monad.MonadError
import sttp.shared.Identity
import sttp.tapir.model.ServerRequest
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

import scala.jdk.CollectionConverters.*

object NdlaTracing {
  private val InstrumentationScope = "no.ndla.network.tapir"

  private val requestHeaderGetter: TextMapGetter[ServerRequest] = new TextMapGetter[ServerRequest] {
    override def keys(carrier: ServerRequest): java.lang.Iterable[String] = carrier.headers.map(_.name).asJava
    override def get(carrier: ServerRequest, key: String): String         = carrier.header(key).orNull
  }

  private def startServerSpan(request: ServerRequest, method: String, route: String): Span = {
    val otel    = GlobalOpenTelemetry.get()
    val parent  = otel.getPropagators.getTextMapPropagator.extract(Context.current(), request, requestHeaderGetter)
    val builder = otel
      .getTracer(InstrumentationScope)
      .spanBuilder(s"$method $route")
      .setSpanKind(SpanKind.SERVER)
      .setParent(parent)
      .setAttribute("http.request.method", method)
      .setAttribute("http.route", route)
    val withCorrelationId = CorrelationID.get match {
      case Some(id) => builder.setAttribute("ndla.correlation_id", id)
      case None     => builder
    }
    withCorrelationId.startSpan()
  }

  private def withServerSpan[A](request: ServerRequest, method: String, route: String, statusCode: A => Option[Int])(
      body: => A
  ): A = {
    val span  = startServerSpan(request, method, route)
    val scope = span.makeCurrent()
    try {
      val result = body
      statusCode(result).foreach { code =>
        val _ = span.setAttribute("http.response.status_code", code.toLong)
        if (code >= 500) {
          val _ = span.setStatus(StatusCode.ERROR)
        }
      }
      result
    } catch {
      case e: Throwable =>
        val _ = span.recordException(e)
        val _ = span.setStatus(StatusCode.ERROR)
        throw e
    } finally {
      scope.close()
      span.end()
    }
  }

  /** Endpoint interceptor that opens a SERVER span around the endpoint execution, named `METHOD /path/template`, and
    * makes it the current context so downstream spans (JDBC, outgoing HTTP, ...) nest under it. By default the agent
    * instruments raw Netty requests, which has no knowledge of Tapir routes, so we disable that and the span is named
    * and attributed here.
    */
  val tracingInterceptor: EndpointInterceptor[Identity] = new EndpointInterceptor[Identity] {
    override def apply[B](
        responder: Responder[Identity, B],
        endpointHandler: EndpointHandler[Identity, B],
    ): EndpointHandler[Identity, B] = new EndpointHandler[Identity, B] {

      override def onDecodeSuccess[A, U, I](
          ctx: DecodeSuccessContext[Identity, A, U, I]
      )(implicit monad: MonadError[Identity], bodyListener: BodyListener[Identity, B]): Identity[ServerResponse[B]] = {
        val method = ctx.request.method.method
        val route  = ctx.endpoint.showPathTemplate(showQueryParam = None)
        withServerSpan(ctx.request, method, route, (r: ServerResponse[B]) => Some(r.code.code)) {
          endpointHandler.onDecodeSuccess(ctx)
        }
      }

      override def onSecurityFailure[A](
          ctx: SecurityFailureContext[Identity, A]
      )(implicit monad: MonadError[Identity], bodyListener: BodyListener[Identity, B]): Identity[ServerResponse[B]] = {
        val method = ctx.request.method.method
        val route  = ctx.endpoint.showPathTemplate(showQueryParam = None)
        withServerSpan(ctx.request, method, route, (r: ServerResponse[B]) => Some(r.code.code)) {
          endpointHandler.onSecurityFailure(ctx)
        }
      }

      override def onDecodeFailure(ctx: DecodeFailureContext)(implicit
          monad: MonadError[Identity],
          bodyListener: BodyListener[Identity, B],
      ): Identity[Option[ServerResponse[B]]] = {
        val method = ctx.request.method.method
        val route  = ctx.endpoint.showPathTemplate(showQueryParam = None)
        withServerSpan(ctx.request, method, route, (r: Option[ServerResponse[B]]) => r.map(_.code.code)) {
          endpointHandler.onDecodeFailure(ctx)
        }
      }
    }
  }
}
