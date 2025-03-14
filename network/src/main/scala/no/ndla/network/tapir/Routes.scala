/*
 * Part of NDLA network
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.sun.net.httpserver.HttpServer
import io.circe.generic.auto.*
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.ndla.common.RequestLogger
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.TaxonomyData
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.NoNullJsonPrinter.*
import org.log4s.{Logger, MDC, getLogger}
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.shared.Identity
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.RequestInterceptor.RequestResultEffectTransform
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.interceptor.reject.{RejectContext, RejectHandler}
import sttp.tapir.server.interceptor.{RequestInterceptor, RequestResult}
import sttp.tapir.server.jdkhttp.{JdkHttpServer, JdkHttpServerOptions}
import sttp.tapir.server.metrics.MetricLabels
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{AttributeKey, EndpointInput, statusCode}

import java.util.concurrent.{ExecutorService, Executors}

trait Routes {
  this: TapirController & HasBaseProps =>

  def services: List[TapirController]

  object Routes {
    val logger: Logger = getLogger
    private def failureResponse(error: String, exception: Option[Throwable]): ValuedEndpointOutput[?] = {
      val logMsg = s"Failure handler got: $error"
      exception match {
        case Some(ex) => logger.error(ex)(logMsg)
        case None     => logger.error(logMsg)
      }

      ValuedEndpointOutput(jsonBody[AllErrors], ErrorHelpers.generic)
    }

    private def decodeFailureHandler[T[_]]: DefaultDecodeFailureHandler[T] =
      DefaultDecodeFailureHandler[T]
        .response(failureMsg => {
          ValuedEndpointOutput(
            jsonBody[AllErrors],
            ErrorHelpers.badRequest(failureMsg)
          )
        })

    private case class NdlaExceptionHandler[T[_]]() extends ExceptionHandler[T] {
      override def apply(ctx: ExceptionContext)(implicit monad: MonadError[T]): T[Option[ValuedEndpointOutput[?]]] = {
        val errorToReturn = returnError(ctx.e)
        val sc            = StatusCode(errorToReturn.statusCode)
        val resp          = ValuedEndpointOutput(jsonBody[AllErrors], errorToReturn)
        val withsc        = resp.prepend(statusCode, sc)
        monad.unit(Some(withsc))
      }
    }

    private def hasMethodMismatch(f: RequestResult.Failure): Boolean = f.failures.map(_.failingInput).exists {
      case _: EndpointInput.FixedMethod[_] => true
      case _                               => false
    }

    case class NdlaRejectHandler[A[_]]() extends RejectHandler[A] {

      override def apply(ctx: RejectContext)(implicit monad: MonadError[A]): A[Option[ValuedEndpointOutput[?]]] = {
        val statusCodeAndBody = if (hasMethodMismatch(ctx.failure)) {
          ValuedEndpointOutput(jsonBody[ErrorBody], ErrorHelpers.methodNotAllowed)
            .prepend(statusCode, StatusCode.MethodNotAllowed)
        } else {
          ValuedEndpointOutput(jsonBody[ErrorBody], ErrorHelpers.notFound)
            .prepend(statusCode, StatusCode.NotFound)
        }
        monad.unit(Some(statusCodeAndBody))
      }
    }

    object JDKMiddleware {
      private def shouldLogRequest(req: ServerRequest): Boolean = {
        if (req.uri.path.size == 1) {
          if (req.uri.path.head == "metrics") return false
          if (req.uri.path.head == "health") return false
        } else if (req.uri.path.size > 1 && req.uri.path.head == "health") return false
        true
      }

      private def setBeforeMDC(info: RequestInfo, req: ServerRequest): Unit = {
        MDC.put("requestPath", RequestLogger.pathWithQueryParams(req)): Unit
        MDC.put("method", req.method.toString()): Unit

        if (info.taxonomyVersion != TaxonomyData.defaultVersion) {
          MDC.put("taxonomyVersion", info.taxonomyVersion): Unit
        }
      }

      val beforeTime = new AttributeKey[Long]("beforeTime")
      def before(req: ServerRequest): ServerRequest = {
        val requestInfo = RequestInfo.fromRequest(req)
        requestInfo.setThreadContextRequestInfo()
        setBeforeMDC(requestInfo, req)
        val startTime = System.currentTimeMillis()

        if (shouldLogRequest(req)) {
          val s = RequestLogger.beforeRequestLogString(req)
          logger.info(s)
        }

        req.attribute(beforeTime, startTime)
      }

      class after extends RequestResultEffectTransform[Identity] {
        def apply[B](req: ServerRequest, result: Identity[RequestResult[B]]): Identity[RequestResult[B]] = {
          if (shouldLogRequest(req)) {
            val code: Int = result match {
              case RequestResult.Response(x) => x.code.code
              case RequestResult.Failure(_)  => -1
            }

            val latency = req
              .attribute(beforeTime)
              .map(startTime => System.currentTimeMillis() - startTime)
              .getOrElse(-1L)

            MDC.put("reqLatencyMs", s"$latency"): Unit

            val s = RequestLogger.afterRequestLogString(
              method = req.method.toString(),
              requestPath = s"/${req.uri.path.mkString("/")}",
              queryString = req.queryParameters.toString(false),
              latency = latency,
              responseCode = code
            )
            logger.info(s)
          }

          RequestInfo.clear()
          MDC.clear()
          result
        }
      }
    }

    private val registry: PrometheusRegistry = new PrometheusRegistry()
    private val metricLabels: MetricLabels = MetricLabels(
      forRequest = List(
        "path"   -> { case (ep, _) => ep.showPathTemplate(showQueryParam = None) },
        "method" -> { case (_, req) => req.method.method }
      ),
      forResponse = List(
        "status" -> {
          case Right(r) => Some(r.code.code.toString)
          case Left(_)  => Some("5xx")
        }
      )
    )

    def startJdkServerAsync(name: String, port: Int)(warmupFunc: => Unit): HttpServer = {
      val prometheusMetrics = PrometheusMetrics
        .default[Identity](namespace = "tapir", registry = registry, labels = metricLabels)

      // val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
      val executor: ExecutorService = Executors.newWorkStealingPool(props.TAPIR_THREADS)

      val options: JdkHttpServerOptions = JdkHttpServerOptions.customiseInterceptors
        .defaultHandlers(err => failureResponse(err, None))
        .rejectHandler(NdlaRejectHandler[Identity]())
        .exceptionHandler(NdlaExceptionHandler[Identity]())
        .decodeFailureHandler(decodeFailureHandler[Identity])
        .serverLog(None)
        .metricsInterceptor(prometheusMetrics.metricsInterceptor())
        .prependInterceptor(RequestInterceptor.transformServerRequest[Identity](JDKMiddleware.before))
        .prependInterceptor(RequestInterceptor.transformResultEffect(new JDKMiddleware.after))
        .options

      val endpoints = services.flatMap(_.builtEndpoints)

      val server = JdkHttpServer()
        .options(options)
        .executor(executor)
        .addEndpoints(endpoints)
        .addEndpoint(prometheusMetrics.metricsEndpoint)
        .port(port)
        .start()

      logger.info(s"Starting $name on port $port")

      warmupFunc

      server
    }

    def startJdkServer(name: String, port: Int)(warmupFunc: => Unit): Unit = {
      startJdkServerAsync(name, port)(warmupFunc): Unit
      // NOTE: Since JdkHttpServer does not block, we need to block the main thread to keep the application alive
      synchronized { wait() }
    }
  }
}
