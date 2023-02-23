/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import no.ndla.common.Clock

import java.time.LocalDateTime
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, ValidationException}
import org.log4s.{Logger, getLogger}
import sttp.model.StatusCode
import io.circe.generic.auto._
import no.ndla.common.errors.NotFoundException
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

import scala.util.{Failure, Success, Try}

sealed trait Error {
  val code: String
  val description: String
  val occuredAt: LocalDateTime
}

case class NotFoundError(code: String, description: String, occuredAt: LocalDateTime)            extends Error
case class GenericError(code: String, description: String, occuredAt: LocalDateTime)             extends Error
case class BadRequestError(code: String, description: String, occuredAt: LocalDateTime)          extends Error
case class UnprocessableEntityError(code: String, description: String, occuredAt: LocalDateTime) extends Error
case class UnauthorizedError(code: String, description: String, occuredAt: LocalDateTime)        extends Error
case class ForbiddenError(code: String, description: String, occuredAt: LocalDateTime)           extends Error

trait ErrorHelpers {
  this: Props with Clock =>

  val NotFoundVariant: EndpointOutput[NotFoundError] =
    statusCode(StatusCode.NotFound).and(jsonBody[NotFoundError])

  val GenericVariant: EndpointOutput[GenericError] =
    statusCode(StatusCode.InternalServerError).and(jsonBody[GenericError])

  val BadRequestVariant: EndpointOutput[BadRequestError] =
    statusCode(StatusCode.BadRequest).and(jsonBody[BadRequestError])

  val UnauthorizedVariant: EndpointOutput[UnauthorizedError] =
    statusCode(StatusCode.Unauthorized).and(jsonBody[UnauthorizedError])

  val ForbiddenVariant: EndpointOutput[ForbiddenError] =
    statusCode(StatusCode.Forbidden).and(jsonBody[ForbiddenError])

  val UnprocessableEntityVariant: EndpointOutput[UnprocessableEntityError] =
    statusCode(StatusCode.UnprocessableEntity).and(jsonBody[UnprocessableEntityError])

  val errorOutputs: EndpointOutput.OneOf[Error, Error] =
    oneOf[Error](
      oneOfVariant(NotFoundVariant),
      oneOfVariant(GenericVariant),
      oneOfVariant(BadRequestVariant),
      oneOfVariant(UnauthorizedVariant),
      oneOfVariant(ForbiddenVariant),
      oneOfVariant(UnprocessableEntityVariant)
    )

  object ErrorHelpers {
    val logger: Logger = getLogger

    val GENERIC              = "GENERIC"
    val NOT_FOUND            = "NOT_FOUND"
    val BAD_REQUEST          = "BAD_REQUEST"
    val UNPROCESSABLE_ENTITY = "UNPROCESSABLE_ENTITY"
    val UNAUTHORIZED         = "UNAUTHORIZED"
    val FORBIDDEN            = "FORBIDDEN"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    val NOT_FOUND_DESCRIPTION = s"The page you requested does not exist"

    val UNAUTHORIZED_DESCRIPTION = "Missing user/client-id or role"
    val FORBIDDEN_DESCRIPTION    = "You do not have the required permissions to access that resource"

    def generic: GenericError                       = GenericError(GENERIC, GENERIC_DESCRIPTION, clock.now())
    def notFound: NotFoundError                     = NotFoundError(NOT_FOUND, NOT_FOUND_DESCRIPTION, clock.now())
    def notFoundWithMsg(msg: String): NotFoundError = NotFoundError(NOT_FOUND, msg, clock.now())
    def badRequest(msg: String = BAD_REQUEST): BadRequestError = BadRequestError(BAD_REQUEST, msg, clock.now())
    def unauthorized: UnauthorizedError = UnauthorizedError(UNAUTHORIZED, UNAUTHORIZED_DESCRIPTION, clock.now())
    def forbidden: ForbiddenError       = ForbiddenError(FORBIDDEN, FORBIDDEN_DESCRIPTION, clock.now())
    def unprocessableEntity(msg: String = UNPROCESSABLE_ENTITY): UnprocessableEntityError =
      UnprocessableEntityError(UNPROCESSABLE_ENTITY, msg, clock.now())

    def returnError(ex: Throwable): Error = {
      ex match {
        case a: ValidationException        => badRequest(ex.getMessage)
        case ex: NotFoundException         => notFoundWithMsg(ex.getMessage)
        case ex: LanguageNotFoundException => notFoundWithMsg(ex.getMessage)
        case ex =>
          logger.error(ex)(s"Internal error: ${ex.getMessage}")
          generic
      }
    }

    implicit class handleErrorOrOkClass[T](t: Try[T]) {
      import cats.implicits._

      /** Function to handle any error If the error is not defined in the default errorHandler [[returnError]] we
        * fallback to a generic 500 error.
        */
      def handleErrorsOrOk: Either[Error, T] = {
        t match {
          case Success(value) => value.asRight
          case Failure(ex)    => ErrorHelpers.returnError(ex).asLeft
        }
      }

      /** Function to override one or more of error responses:
        * {{{
        *     someMethodThatReturnsTry().partialOverride { case x: SomeExceptionToHandle =>
        *         ErrorHelpers.unprocessableEntity("Cannot process")
        *     }
        * }}}
        *
        * If the error is not defined in the callback or in the default errorHandler [[returnError]] we fallback to a
        * generic 500 error.
        */
      def partialOverride(callback: PartialFunction[Throwable, Error]): Either[Error, T] = {
        t match {
          case Success(value)                          => value.asRight
          case Failure(ex) if callback.isDefinedAt(ex) => callback(ex).asLeft
          case Failure(ex)                             => ErrorHelpers.returnError(ex).asLeft
        }
      }

    }
  }
}
