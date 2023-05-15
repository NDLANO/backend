/*
 * Part of NDLA network
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import no.ndla.network.tapir.NoNullJsonPrinter._
import cats.data.Kleisli
import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.headers.`Content-Type`
import org.http4s.server.Router
import org.http4s.{Headers, HttpRoutes, MediaType, Request, Response}
import org.log4s.{Logger, getLogger}
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.interceptor.reject.RejectHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{EndpointInput, statusCode}

trait Routes {
  this: Service with NdlaMiddleware with TapirErrorHelpers =>

  object Routes {
    val logger: Logger = getLogger
    private def buildBindings(routes: List[Service]): List[(String, HttpRoutes[IO])] = {
      val (docServices, noDocServices) = routes.partitionMap {
        case swaggerService: SwaggerService  => Left(swaggerService)
        case serviceWithoutDoc: NoDocService => Right(serviceWithoutDoc)
      }

      // Full paths are already prefixed in the endpoints to make nice documentation
      val swaggerBinding = "/" -> swaggerServicesToRoutes(docServices)
      noDocServices.map(_.getBinding) :+ swaggerBinding
    }

    private def failureResponse(error: String, exception: Option[Throwable]): ValuedEndpointOutput[_] = {
      val logMsg = s"Failure handler got: $error"
      exception match {
        case Some(ex) => logger.error(ex)(logMsg)
        case None     => logger.error(logMsg)
      }

      ValuedEndpointOutput(jsonBody[ErrorBody], ErrorHelpers.generic)
    }

    private val decodeFailureHandler =
      DefaultDecodeFailureHandler.default
        .response(failureMsg => {
          ValuedEndpointOutput(
            jsonBody[ErrorBody],
            ErrorHelpers.badRequest(failureMsg)
          )
        })

    private case class NdlaExceptionHandler() extends ExceptionHandler[IO] {
      override def apply(ctx: ExceptionContext)(implicit monad: MonadError[IO]): IO[Option[ValuedEndpointOutput[_]]] = {
        val errorToReturn = returnError(ctx.e)
        val sc            = StatusCode(errorToReturn.statusCode)

        val resp   = ValuedEndpointOutput(jsonBody[ErrorBody], errorToReturn)
        val withsc = resp.prepend(statusCode, sc)
        monad.unit(Some(withsc))
      }
    }

    private def hasMethodMismatch(f: RequestResult.Failure): Boolean = f.failures.map(_.failingInput).exists {
      case _: EndpointInput.FixedMethod[_] => true
      case _                               => false
    }

    private case class NdlaRejectHandler[F[_]]() extends RejectHandler[F] {
      override def apply(
          failure: RequestResult.Failure
      )(implicit monad: MonadError[F]): F[Option[ValuedEndpointOutput[_]]] = {
        val statusCodeAndBody = if (hasMethodMismatch(failure)) {
          ValuedEndpointOutput(jsonBody[ErrorBody], ErrorHelpers.methodNotAllowed)
            .prepend(statusCode, StatusCode.MethodNotAllowed)
        } else {
          ValuedEndpointOutput(jsonBody[ErrorBody], ErrorHelpers.notFound)
            .prepend(statusCode, StatusCode.NotFound)
        }
        monad.unit(Some(statusCodeAndBody))
      }

    }

    private def swaggerServicesToRoutes(services: List[SwaggerService]): HttpRoutes[IO] = {
      val swaggerEndpoints = services.flatMap(_.builtEndpoints)
      val options = Http4sServerOptions
        .customiseInterceptors[IO]
        .defaultHandlers(err => failureResponse(err, None))
        .rejectHandler(NdlaRejectHandler[IO]())
        .exceptionHandler(NdlaExceptionHandler())
        .decodeFailureHandler(decodeFailureHandler)
        .options
      Http4sServerInterpreter[IO](options).toRoutes(swaggerEndpoints)
    }

    private def getFallbackRoute: Response[IO] = {
      val headers = Headers(`Content-Type`(MediaType.application.json))
      Response.notFound[IO].withEntity(ErrorHelpers.notFound).withHeaders(headers)
    }

    def build(routes: List[Service]): Kleisli[IO, Request[IO], Response[IO]] = {
      logger.info("Building swagger service")
      val bindings = buildBindings(routes)
      val router   = Router[IO](bindings: _*)
      Kleisli[IO, Request[IO], Response[IO]](req => {
        val ran = router.run(req)
        val res = ran.getOrElse { getFallbackRoute }
        NdlaMiddleware(req, res)
      })
    }

  }
}
