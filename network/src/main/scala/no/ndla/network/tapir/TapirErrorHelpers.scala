/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import org.log4s.{Logger, getLogger}
import scala.util.{Try, Success, Failure}

trait TapirErrorHelpers {
  this: HasBaseProps with Clock =>

  object ErrorHelpers {
    val logger: Logger = getLogger

    val GENERIC              = "GENERIC"
    val NOT_FOUND            = "NOT_FOUND"
    val BAD_REQUEST          = "BAD_REQUEST"
    val UNPROCESSABLE_ENTITY = "UNPROCESSABLE_ENTITY"
    val UNAUTHORIZED         = "UNAUTHORIZED"
    val FORBIDDEN            = "FORBIDDEN"

    val PARAMETER_MISSING      = "PARAMETER MISSING"
    val PROVIDER_NOT_SUPPORTED = "PROVIDER NOT SUPPORTED"
    val REMOTE_ERROR           = "REMOTE ERROR"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    val NOT_FOUND_DESCRIPTION = s"The page you requested does not exist"

    val UNAUTHORIZED_DESCRIPTION = "Missing user/client-id or role"
    val FORBIDDEN_DESCRIPTION    = "You do not have the required permissions to access that resource"

    def generic: GenericBody                       = GenericBody(GENERIC, GENERIC_DESCRIPTION, clock.now())
    def notFound: NotFoundBody                     = NotFoundBody(NOT_FOUND, NOT_FOUND_DESCRIPTION, clock.now())
    def notFoundWithMsg(msg: String): NotFoundBody = NotFoundBody(NOT_FOUND, msg, clock.now())
    def badRequest(msg: String): BadRequestBody    = BadRequestBody(BAD_REQUEST, msg, clock.now())
    def unauthorized: UnauthorizedBody = UnauthorizedBody(UNAUTHORIZED, UNAUTHORIZED_DESCRIPTION, clock.now())
    def forbidden: ForbiddenBody       = ForbiddenBody(FORBIDDEN, FORBIDDEN_DESCRIPTION, clock.now())
    def unprocessableEntity(msg: String): UnprocessableEntityBody =
      UnprocessableEntityBody(UNPROCESSABLE_ENTITY, msg, clock.now())
  }

  def returnError(ex: Throwable): ErrorBody

  implicit class handleErrorOrOkClass[T](t: Try[T]) {
    import cats.implicits._

    /** Function to handle any error If the error is not defined in the default errorHandler [[returnError]] we fallback
      * to a generic 500 error.
      */
    def handleErrorsOrOk: Either[ErrorBody, T] = t match {
      case Success(value) => value.asRight
      case Failure(ex)    => returnError(ex).asLeft
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
    def partialOverride(callback: PartialFunction[Throwable, ErrorBody]): Either[ErrorBody, T] = t match {
      case Success(value)                          => value.asRight
      case Failure(ex) if callback.isDefinedAt(ex) => callback(ex).asLeft
      case Failure(ex)                             => returnError(ex).asLeft
    }

  }
}
