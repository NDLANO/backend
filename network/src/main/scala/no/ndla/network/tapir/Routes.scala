/*
 * Part of NDLA network
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.data.Kleisli
import cats.effect.IO
import io.circe.generic.auto._
import no.ndla.common.RequestLogger
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.NoNullJsonPrinter._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.headers.`Content-Type`
import org.http4s.server.Router
import org.http4s.{Headers, HttpRoutes, MediaType, Request, Response}
import org.log4s.{Logger, getLogger}
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.RequestInterceptor.RequestResultEffectTransform
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.interceptor.reject.{RejectContext, RejectHandler}
import sttp.tapir.server.interceptor.{RequestInterceptor, RequestResult}
import sttp.tapir.server.jdkhttp.{Id, JdkHttpServer, JdkHttpServerOptions}
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{AttributeKey, EndpointInput, statusCode}

import java.util.concurrent.{ExecutorService, Executors}

trait Routes[F[_]] {
  this: NdlaMiddleware with TapirErrorHelpers with HasBaseProps =>

  val services: List[Service[F]]

  object Routes {
    val logger: Logger = getLogger
    private def failureResponse(error: String, exception: Option[Throwable]): ValuedEndpointOutput[_] = {
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
      override def apply(ctx: ExceptionContext)(implicit monad: MonadError[T]): T[Option[ValuedEndpointOutput[_]]] = {
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

      override def apply(ctx: RejectContext)(implicit monad: MonadError[A]): A[Option[ValuedEndpointOutput[_]]] = {
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

    private def swaggerServicesToRoutes(services: List[Service[IO]]): HttpRoutes[IO] = {
      val swaggerEndpoints = services.flatMap(_.builtEndpoints)
      val options = Http4sServerOptions
        .customiseInterceptors[IO]
        .defaultHandlers(err => failureResponse(err, None))
        .rejectHandler(NdlaRejectHandler[IO]())
        .exceptionHandler(NdlaExceptionHandler[IO]())
        .decodeFailureHandler(decodeFailureHandler[IO])
        .serverLog(None)
        .options
      Http4sServerInterpreter[IO](options).toRoutes(swaggerEndpoints)
    }

    private def getFallbackRoute: Response[IO] = {
      val headers = Headers(`Content-Type`(MediaType.application.json))
      Response.notFound[IO].withEntity(ErrorHelpers.notFound).withHeaders(headers)
    }

    private def build(routes: List[Service[IO]]): Kleisli[IO, Request[IO], Response[IO]] = {
      logger.info("Building swagger service")
      val bindings = "/" -> swaggerServicesToRoutes(routes)
      val router   = Router[IO](bindings)
      Kleisli[IO, Request[IO], Response[IO]](req => {
        val ran = router.run(req)
        val res = ran.getOrElse { getFallbackRoute }
        NdlaMiddleware(req, res)
      })
    }

    object JDKMiddleware {
      private def shouldLogRequest(req: ServerRequest): Boolean = s"/${req.uri.path.mkString("/")}" != "/health"

      val beforeTime = new AttributeKey[Long]("beforeTime")
      def before(req: ServerRequest) = {
        val requestInfo = RequestInfo.fromRequest(req)
        requestInfo.setThreadContextRequestInfo()
        val startTime = System.currentTimeMillis()

        if (shouldLogRequest(req)) {
          val s = RequestLogger.beforeRequestLogString(
            method = req.method.toString(),
            requestPath = s"/${req.uri.path.mkString("/")}",
            queryString = req.queryParameters.toString(false)
          )
          logger.info(s)
        }

        req.attribute(beforeTime, startTime)
      }

      class after extends RequestResultEffectTransform[Id] {
        def apply[B](req: ServerRequest, result: Id[RequestResult[B]]): Id[RequestResult[B]] = {
          if (shouldLogRequest(req)) {
            val code: Int = result match {
              case RequestResult.Response(x) => x.code.code
              case RequestResult.Failure(_)  => -1
            }

            val latency = req
              .attribute(beforeTime)
              .map(startTime => System.currentTimeMillis() - startTime)
              .getOrElse(-1L)

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
          result
        }
      }
    }

    def startHttp4sServer(name: String, port: Int)(warmupFunc: => Unit): IO[Unit] = {
      val app: Kleisli[IO, Request[IO], Response[IO]] = Routes.build(services.asInstanceOf[List[Service[IO]]])
      val server: TapirServer                         = TapirServer(name, port, app, enableMelody = true)(warmupFunc)
      logger.info(s"Starting $name on port $port")
      server.as(())
    }

    def startJdkServer(name: String, port: Int)(warmupFunc: => Unit): Unit = {
      // val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
      val executor: ExecutorService = Executors.newWorkStealingPool(props.TAPIR_THREADS)

      val options: JdkHttpServerOptions = JdkHttpServerOptions.customiseInterceptors
        .defaultHandlers(err => failureResponse(err, None))
        .rejectHandler(NdlaRejectHandler[Id]())
        .exceptionHandler(NdlaExceptionHandler[Id]())
        .decodeFailureHandler(decodeFailureHandler[Id])
        .serverLog(None)
        .prependInterceptor(RequestInterceptor.transformServerRequest[Id](JDKMiddleware.before))
        .prependInterceptor(RequestInterceptor.transformResultEffect(new JDKMiddleware.after))
        .options

      val endpoints = services.asInstanceOf[List[Service[Id]]].flatMap(_.builtEndpoints)

      JdkHttpServer()
        .options(options)
        .executor(executor)
        .addEndpoints(endpoints)
        .port(port)
        .start(): Unit

      logger.info(s"Starting $name on port $port")

      warmupFunc

      // NOTE: Since JdkHttpServer does not block, we need to block the main thread to keep the application alive
      synchronized { wait() }
    }
  }
}
