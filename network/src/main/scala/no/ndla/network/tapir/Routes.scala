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
import no.ndla.common.configuration.BaseProps
import no.ndla.common.{CorrelationID, RequestLogger}
import no.ndla.common.configuration.Constants
import no.ndla.network.TaxonomyData
import no.ndla.network.model.*
import no.ndla.network.tapir.NoNullJsonPrinter.*
import org.playframework.netty.http.StreamedHttpRequest
import org.slf4j.MDC
import ox.channels.{Channel, ChannelClosed}
import ox.{Chunk, never, supervised, useInScope}
import sttp.model.HeaderNames.SensitiveHeaders
import sttp.model.{Header, HeaderNames, StatusCode}
import sttp.monad.MonadError
import sttp.shared.Identity
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.RequestInterceptor.RequestResultEffectTransform
import sttp.tapir.server.interceptor.decodefailure.{DecodeFailureHandler, DefaultDecodeFailureHandler}
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.interceptor.reject.{RejectContext, RejectHandler}
import sttp.tapir.server.interceptor.{DecodeFailureContext, RequestInterceptor, RequestResult}
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.netty.NettyConfig
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerBinding, NettySyncServerOptions}
import sttp.tapir.{AttributeKey, DecodeResult, EndpointInput, headers, statusCode}

import scala.concurrent.duration.DurationInt

class Routes(using
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
    services: List[TapirController],
    props: BaseProps,
) extends StrictLogging {
  private def failureResponse(error: String, exception: Option[Throwable]): ValuedEndpointOutput[?] = {
    val logMsg = s"Failure handler got: $error"
    exception match {
      case Some(ex) => logger.error(logMsg, ex)
      case None     => logger.error(logMsg)
    }

    ValuedEndpointOutput(jsonBody[AllErrors], errorHelpers.generic)
  }

  private object NdlaDecodeFailureHandler extends DecodeFailureHandler[Identity] {
    override def apply(ctx: DecodeFailureContext)(implicit
        monad: MonadError[Identity]
    ): Identity[Option[ValuedEndpointOutput[((StatusCode, List[Header]), AllErrors)]]] = ctx.failure match {
      case DecodeResult.Error(_, ex: AuthException) => handleAuthFailure(ex)
      case _                                        =>
        val res = DefaultDecodeFailureHandler
          .respond(ctx)
          .map { case (sc, hs) =>
            val failureMsg      = DefaultDecodeFailureHandler.FailureMessages.failureMessage(ctx)
            val errorBodyOutput = ValuedEndpointOutput(jsonBody[AllErrors], errorHelpers.badRequest(failureMsg))
            errorBodyOutput.prepend(statusCode.and(headers), (sc, hs))
          }
        monad.unit(res)
    }

    private def handleAuthFailure(
        ex: AuthException
    ): Option[ValuedEndpointOutput[((StatusCode, List[Header]), AllErrors)]] = {
      val (sc, body) = ex match {
        case _: MissingAudienceConfiguration =>
          logger.error(ex.message, ex)
          (StatusCode.InternalServerError, errorHelpers.generic)
        case _: UnexpectedNimbusException =>
          logger.error(ex.message, ex)
          (StatusCode.InternalServerError, errorHelpers.generic)
        case _: GetFeideUserWrapperException =>
          logger.error(ex.message, ex)
          (StatusCode.InternalServerError, errorHelpers.generic)
        case _: InvalidJwtException =>
          logger.error(ex.message, ex)
          (StatusCode.Unauthorized, errorHelpers.unauthorized.copy(description = ex.message))
        case _: UnauthenticatedException =>
          (StatusCode.Unauthorized, errorHelpers.unauthorized.copy(description = ex.message))
        case _: ForbiddenException => (StatusCode.Forbidden, errorHelpers.forbidden.copy(description = ex.message))
      }
      val errorBodyOutput = ValuedEndpointOutput(jsonBody[AllErrors], body)
      Some(errorBodyOutput.prepend(statusCode.and(headers), (sc, Nil)))
    }
  }

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

      def apply[B](req: ServerRequest, result: RequestResult[B]): RequestResult[B] = {
        if (req.attribute(activityTracked).contains(true)) {
          val code: Int = result match {
            case RequestResult.Response(response, _) => response.code.code
            case RequestResult.Failure(failures)     => -1
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

        val response = withCorrelationIdHeader(result)

        RequestInfo.clear()
        MDC.clear()

        response
      }
    }
  }

  private def withCorrelationIdHeader[B](result: RequestResult[B]): RequestResult[B] = {
    CorrelationID.get match {
      case None                => result
      case Some(correlationId) => result match {
          case RequestResult.Response(response, source) =>
            val header = Header(Constants.CorrelationIdHeader, correlationId)
            RequestResult.Response(response.addHeaders(List(header)), source)
          case other => other
        }
    }
  }

  def startServerAndWait(name: String, port: Int)(onStartup: NettySyncServerBinding => Unit): Unit = {
    val prometheusMetrics = NdlaPrometheusRegistry.tapirPrometheusMetrics

    val options = NettySyncServerOptions
      .customiseInterceptors
      .defaultHandlers(err => failureResponse(err, None))
      .rejectHandler(NdlaRejectHandler[Identity]())
      .exceptionHandler(NdlaExceptionHandler[Identity]())
      .decodeFailureHandler(NdlaDecodeFailureHandler)
      .serverLog(None)
      .metricsInterceptor(prometheusMetrics.metricsInterceptor())
      .addInterceptor(NdlaTracing.tracingInterceptor)
      .prependInterceptor(TapirMiddleware.before)
      .prependInterceptor(RequestInterceptor.transformResultEffect(new TapirMiddleware.after))
      .options

    val gracefulShutdownTimeout = 30.seconds

    val config = NettyConfig
      .default
      .copy(gracefulShutdownTimeout = Option.when(props.Environment != "local")(gracefulShutdownTimeout))
      .connectionTimeout(30.seconds) // Use same connection timeout as Netty default
      .requestTimeout(55.minutes)    // Allow for long-running requests (e.g., internal indexing endpoints)
      .idleTimeout(60.minutes)       // Same as the comment above
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
}
