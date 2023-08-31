/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.logging.FLogging
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import sttp.monad.MonadError
import sttp.tapir.Endpoint
import sttp.tapir.server.PartialServerEndpoint

import scala.util.{Failure, Success, Try}

trait TapirErrorHelpers extends FLogging {
  this: HasBaseProps with Clock =>

  object ErrorHelpers {
    val GENERIC                = "GENERIC"
    val NOT_FOUND              = "NOT_FOUND"
    val BAD_REQUEST            = "BAD_REQUEST"
    val INDEX_MISSING          = "INDEX_MISSING"
    val UNPROCESSABLE_ENTITY   = "UNPROCESSABLE_ENTITY"
    val UNAUTHORIZED           = "UNAUTHORIZED"
    val FORBIDDEN              = "FORBIDDEN"
    val ACCESS_DENIED          = "ACCESS DENIED"
    val IMPORT_FAILED          = "IMPORT_FAILED"
    val FILE_TOO_BIG           = "FILE TOO BIG"
    val DATABASE_UNAVAILABLE   = "DATABASE_UNAVAILABLE"
    val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"
    val VALIDATION             = "VALIDATION_ERROR"
    val METHOD_NOT_ALLOWED     = "METHOD_NOT_ALLOWED"

    val PARAMETER_MISSING      = "PARAMETER MISSING"
    val PROVIDER_NOT_SUPPORTED = "PROVIDER NOT SUPPORTED"
    val REMOTE_ERROR           = "REMOTE ERROR"
    val WINDOW_TOO_LARGE       = "RESULT_WINDOW_TOO_LARGE"
    val RESOURCE_OUTDATED      = "RESOURCE_OUTDATED"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    val NOT_FOUND_DESCRIPTION            = s"The page you requested does not exist"
    val DATABASE_UNAVAILABLE_DESCRIPTION = s"Database seems to be unavailable, retrying connection."
    val UNAUTHORIZED_DESCRIPTION         = "Missing user/client-id or role"
    val FORBIDDEN_DESCRIPTION            = "You do not have the required permissions to access that resource"
    val RESOURCE_OUTDATED_DESCRIPTION    = "The resource is outdated. Please try fetching before submitting again."
    val METHOD_NOT_ALLOWED_DESCRIPTION   = "You requested a unsupported method on this endpoint."
    val VALIDATION_DESCRIPTION           = "Validation Error"
    val INVALID_SEARCH_CONTEXT_DESCRIPTION =
      "The search-context specified was not expected. Please create one by searching from page 1."
    val INDEX_MISSING_DESCRIPTION =
      s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${props.ContactEmail} if the error persists."

    def generic: ErrorBody                      = ErrorBody(GENERIC, GENERIC_DESCRIPTION, clock.now(), 500)
    def notFound: ErrorBody                     = ErrorBody(NOT_FOUND, NOT_FOUND_DESCRIPTION, clock.now(), 404)
    def notFoundWithMsg(msg: String): ErrorBody = ErrorBody(NOT_FOUND, msg, clock.now(), 404)
    def badRequest(msg: String): ErrorBody      = ErrorBody(BAD_REQUEST, msg, clock.now(), 400)
    def unauthorized: ErrorBody                 = ErrorBody(UNAUTHORIZED, UNAUTHORIZED_DESCRIPTION, clock.now(), 401)
    def forbidden: ErrorBody                    = ErrorBody(FORBIDDEN, FORBIDDEN_DESCRIPTION, clock.now(), 403)
    def unprocessableEntity(msg: String): ErrorBody = ErrorBody(UNPROCESSABLE_ENTITY, msg, clock.now(), 422)
    def invalidSearchContext: ErrorBody =
      ErrorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, clock.now(), 400)
    def methodNotAllowed: ErrorBody = ErrorBody(METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_DESCRIPTION, clock.now(), 405)

    /** Helper function that returns function one can pass to `serverSecurityLogicPure` to require a specific scope for
      * some endpoint.
      */
    def requireScope(scope: Permission*): Option[TokenUser] => Either[AllErrors, TokenUser] = {
      case Some(user) if user.hasPermissions(scope) => user.asRight
      case Some(_)                                  => ErrorHelpers.forbidden.asLeft
      case None                                     => ErrorHelpers.unauthorized.asLeft
    }

    implicit class authlessEndpoint[A, I, E, O, R](self: Endpoint[Unit, I, AllErrors, O, R]) {
      def requirePermission[F[_]](
          requiredPermission: Permission*
      ): PartialServerEndpoint[Option[TokenUser], TokenUser, I, AllErrors, O, R, F] = {
        val newEndpoint   = self.securityIn(sttp.tapir.auth.bearer[Option[TokenUser]]())
        val authFunc      = ErrorHelpers.requireScope(requiredPermission: _*)
        val securityLogic = (m: MonadError[F]) => (a: Option[TokenUser]) => m.unit(authFunc(a))

        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }

    implicit class authlessErrorlessEndpoint[A, I, E, O, R, X](self: Endpoint[Unit, I, X, O, R]) {
      def withOptionalUser[F[_]]: PartialServerEndpoint[Option[TokenUser], Option[TokenUser], I, X, O, R, F] = {
        val newEndpoint   = self.securityIn(sttp.tapir.auth.bearer[Option[TokenUser]]())
        val authFunc      = (tokenUser: Option[TokenUser]) => Right(tokenUser): Either[X, Option[TokenUser]]
        val securityLogic = (m: MonadError[F]) => (a: Option[TokenUser]) => m.unit(authFunc(a))
        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }

  }

  private def handleUnknownError(e: Throwable): IO[ErrorBody] = {
    logger.error(e.getMessage).as {
      e.printStackTrace()
      ErrorHelpers.generic
    }
  }

  def handleErrors: PartialFunction[Throwable, IO[AllErrors]]
  def returnError(ex: Throwable): IO[AllErrors] = handleErrors.applyOrElse(ex, handleUnknownError)

  def returnLeftError[R](ex: Throwable): IO[Either[AllErrors, R]] = returnError(ex).map(_.asLeft[R])

  implicit class handleErrorOrOkClass[T](t: Try[T]) {
    import cats.implicits._

    /** Function to handle any error If the error is not defined in the default errorHandler [[returnError]] we fallback
      * to a generic 500 error.
      */
    def handleErrorsOrOk: IO[Either[AllErrors, T]] = t match {
      case Success(value) => IO(value.asRight)
      case Failure(ex)    => returnLeftError(ex)
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
    def partialOverride(callback: PartialFunction[Throwable, ErrorBody]): IO[Either[AllErrors, T]] = t match {
      case Success(value)                          => IO(value.asRight)
      case Failure(ex) if callback.isDefinedAt(ex) => IO(callback(ex).asLeft)
      case Failure(ex)                             => returnLeftError(ex)
    }

  }
}
