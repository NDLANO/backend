/*
 * Part of NDLA network
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto.*
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.ndla.common.RequestLogger
import no.ndla.network.TaxonomyData
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.NoNullJsonPrinter.*
import org.playframework.netty.http.StreamedHttpRequest
import org.slf4j.MDC
import ox.channels.{Channel, ChannelClosed}
import ox.{Chunk, never, supervised, useInScope}
import sttp.model.HeaderNames.SensitiveHeaders
import sttp.model.{HeaderNames, StatusCode}
import sttp.monad.MonadError
import sttp.shared.Identity
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.RequestInterceptor.RequestResultEffectTransform
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.interceptor.reject.{RejectContext, RejectHandler}
import sttp.tapir.server.interceptor.{CustomiseInterceptors, RequestInterceptor, RequestResult}
import sttp.tapir.server.metrics.MetricLabels
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.netty.NettyConfig
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerBinding, NettySyncServerOptions}
import sttp.tapir.{AttributeKey, EndpointInput, statusCode}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class Routes(using errorHelpers: ErrorHelpers, errorHandling: ErrorHandling, services: List[TapirController])
    extends StrictLogging {
  private def failureResponse(error: String, exception: Option[Throwable]): ValuedEndpointOutput[?] = {
    val logMsg = s"Failure handler got: $error"
    exception match {
      case Some(ex) => logger.error(logMsg, ex)
      case None     => logger.error(logMsg)
    }

    ValuedEndpointOutput(jsonBody[AllErrors], errorHelpers.generic)
  }

  private def decodeFailureHandler[T[_]]: DefaultDecodeFailureHandler[T] =
    DefaultDecodeFailureHandler[T].response(failureMsg => {
      ValuedEndpointOutput(jsonBody[AllErrors], errorHelpers.badRequest(failureMsg))
    })

  private case class NdlaExceptionHandler[T[_]]() extends ExceptionHandler[T] {
    override def apply(ctx: ExceptionContext)(implicit monad: MonadError[T]): T[Option[ValuedEndpointOutput[?]]] = {
      val errorToReturn = errorHandling.returnError(ctx.e)
      val sc            = StatusCode(errorToReturn.statusCode)
      val resp          = ValuedEndpointOutput(jsonBody[AllErrors], errorToReturn)
      val withsc        = resp.prepend(statusCode, sc)
      monad.unit(Some(withsc))
    }
  }

  private def hasMethodMismatch(f: RequestResult.Failure): Boolean = f
    .failures
    .map(_.failingInput)
    .exists {
      case _: EndpointInput.FixedMethod[_] => true
      case _                               => false
    }

  private case class NdlaRejectHandler[A[_]]() extends RejectHandler[A] {

    override def apply(ctx: RejectContext)(implicit monad: MonadError[A]): A[Option[ValuedEndpointOutput[?]]] = {
      val statusCodeAndBody =
        if (hasMethodMismatch(ctx.failure)) {
          ValuedEndpointOutput(jsonBody[ErrorBody], errorHelpers.methodNotAllowed).prepend(
            statusCode,
            StatusCode.MethodNotAllowed,
          )
        } else {
          ValuedEndpointOutput(jsonBody[ErrorBody], errorHelpers.notFound).prepend(statusCode, StatusCode.NotFound)
        }
      monad.unit(Some(statusCodeAndBody))
    }
  }

  private object TapirMiddleware {
    private def shouldLogRequest(req: ServerRequest): Boolean = {
      if (req.uri.path.size == 1) {
        if (req.uri.path.head == "metrics") return false
        if (req.uri.path.head == "health") return false
      } else if (req.uri.path.size > 1 && req.uri.path.head == "health") return false
      true
    }

    private def setBeforeMDC(info: RequestInfo, req: ServerRequest): Unit = {
      MDC.put("requestPath", RequestLogger.pathWithQueryParams(req))
      MDC.put("method", req.method.toString())

      if (info.taxonomyVersion != TaxonomyData.defaultVersion) {
        MDC.put("taxonomyVersion", info.taxonomyVersion): Unit
      }
    }

    private val beforeTime      = new AttributeKey[Long]("beforeTime")
    private val activityTracked = new AttributeKey[Boolean]("activityTracked")
    private val requestBody     = new AttributeKey[Channel[Chunk[Byte]]]("requestBody")

    private val requestBodyLoggingCutoff     = 1 * 1024 * 1024 // 1 MB
    def before: RequestInterceptor[Identity] = RequestInterceptor.transformServerRequest { req =>
      val requestInfo = RequestInfo.fromRequest(req)
      requestInfo.setThreadContextRequestInfo()
      setBeforeMDC(requestInfo, req)
      val startTime = System.currentTimeMillis()

      val shouldLog = shouldLogRequest(req)
      if (shouldLog) {
        logger.info(RequestLogger.beforeRequestLogString(req))
      }

      val bodyLoggingRequest = req.underlying match {
        case sr: StreamedHttpRequest =>
          val requestBodyChannel = Channel.unlimited[Chunk[Byte]]
          val newUnderlying      = NettyStreamedRequestWrapper(sr, requestBodyChannel, requestBodyLoggingCutoff)
          req.withUnderlying(newUnderlying).attribute(requestBody, requestBodyChannel)

        case _ => req
      }

      bodyLoggingRequest.attribute(beforeTime, startTime).attribute(activityTracked, shouldLog)
    }

    class after extends RequestResultEffectTransform[Identity] {
      private val sensitiveHeaders                       = SensitiveHeaders + "feideauthorization"
      private def addHeaderMDC(req: ServerRequest): Unit = req
        .headers
        .foreach { header =>
          val value =
            if (HeaderNames.isSensitive(header, sensitiveHeaders)) "[REDACTED]"
            else header.value
          MDC.put(s"requestHeader.${header.name.toLowerCase}", value)
        }

      private def addRequestBodyMDC(req: ServerRequest): Unit = req
        .attribute(requestBody)
        .foreach { bodyChannel =>
          val sb = StringBuilder()
          bodyChannel.doneOrClosed(): Unit
          bodyChannel.foreachOrError(chunk => sb.append(chunk.asStringUtf8)) match {
            case ChannelClosed.Error(t) => logger.warn("Error reading request body for logging", t)
            case ()                     =>
              val body = sb.result()
              MDC.put("requestBody", body)
          }
        }

      def apply[B](req: ServerRequest, result: Identity[RequestResult[B]]): Identity[RequestResult[B]] = {
        if (req.attribute(activityTracked).contains(true)) {
          val code: Int = result match {
            case RequestResult.Response(x) => x.code.code
            case RequestResult.Failure(_)  => -1
          }

          val latency = req
            .attribute(beforeTime)
            .map(startTime => System.currentTimeMillis() - startTime)
            .getOrElse(-1L)

          if (code >= 400) {
            addRequestBodyMDC(req)
            addHeaderMDC(req)
          }

          MDC.put("reqLatencyMs", s"$latency")
          MDC.put("statusCode", code.toString)

          val s = RequestLogger.afterRequestLogString(
            method = req.method.toString(),
            requestPath = s"/${req.uri.path.mkString("/")}",
            queryString = req.queryParameters.toString(false),
            latency = latency,
            responseCode = code,
          )

          if (code >= 500) logger.error(s)
          else logger.info(s)
        }

        RequestInfo.clear()
        MDC.clear()
        result
      }
    }
  }

  def startServerAndWait(name: String, port: Int, gracefulShutdownTimeout: FiniteDuration = 30.seconds)(
      onStartup: NettySyncServerBinding => Unit
  ): Unit = {
    val prometheusMetrics =
      PrometheusMetrics.default[Identity](namespace = "tapir", registry = registry, labels = metricLabels)

    val options = NettySyncServerOptions
      .customiseInterceptors
      .defaultHandlers(err => failureResponse(err, None))
      .rejectHandler(NdlaRejectHandler[Identity]())
      .exceptionHandler(NdlaExceptionHandler[Identity]())
      .decodeFailureHandler(decodeFailureHandler[Identity])
      .serverLog(None)
      .metricsInterceptor(prometheusMetrics.metricsInterceptor())
      .prependInterceptor(TapirMiddleware.before)
      .prependInterceptor(RequestInterceptor.transformResultEffect(new TapirMiddleware.after))
      .options
      .copy(interruptServerLogicWhenRequestCancelled = false)

    val config = NettyConfig
      .default
      .withGracefulShutdownTimeout(gracefulShutdownTimeout)
      .connectionTimeout(30.seconds) // Use same connection timeout as Netty default
      .requestTimeout(55.minutes)    // Allow for long-running requests (e.g., internal indexing endpoints)
      .idleTimeout(60.minutes)       // --||--
    val endpoints = services.flatMap(_.builtEndpoints)

    logger.info(s"Starting $name on port $port")

    supervised {
      val serverBinding = useInScope(
        NettySyncServer(options, config)
          .addEndpoints(endpoints)
          .addEndpoint(prometheusMetrics.metricsEndpoint)
          .host("0.0.0.0")
          .port(port)
          .start()
      )(_.stop())
      onStartup(serverBinding)
      never
    }
  }

  private[tapir] val registry: PrometheusRegistry = new PrometheusRegistry()
  private val metricLabels: MetricLabels          = MetricLabels(
    forRequest = List(
      "path" -> { case (ep, _) =>
        ep.showPathTemplate(showQueryParam = None)
      },
      "method" -> { case (_, req) =>
        req.method.method
      },
    ),
    forResponse = List(
      "status" -> {
        case Right(r) => Some(r.code.code.toString)
        case Left(ex: RuntimeException)
            if ex.getMessage == "Client disconnected, request timed out, or request cancelled" => Some("499")
        case Left(_) => Some("5xx")
      }
    ),
  )
}
