/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseProps
import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}

trait TapirErrorHandling(using props: BaseProps, clock: Clock, errorHelpers: ErrorHelpers) extends StrictLogging {
  def logError(e: Throwable): Unit = {
    logger.error(e.getMessage, e)
  }

  private def handleUnknownError(e: Throwable): ErrorBody = {
    logError(e)
    errorHelpers.generic
  }

  def handleErrors: PartialFunction[Throwable, AllErrors]
  def returnError(ex: Throwable): AllErrors                    = handleErrors.applyOrElse(ex, handleUnknownError)
  def returnLeftError[R](ex: Throwable): Either[AllErrors, R]  = returnError(ex).asLeft[R]
  implicit def tryToEither[T](x: Try[T]): Either[AllErrors, T] = x.handleErrorsOrOk

  implicit class handleErrorOrOkClass[T](t: Try[T]) {
    import cats.implicits.*

    /** Function to handle any error If the error is not defined in the default errorHandler [[returnError]] we fallback
      * to a generic 500 error.
      */
    def handleErrorsOrOk: Either[AllErrors, T] = t match {
      case Success(value) => value.asRight
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
    def partialOverride(callback: PartialFunction[Throwable, ErrorBody]): Either[AllErrors, T] = t match {
      case Success(value)                          => value.asRight
      case Failure(ex) if callback.isDefinedAt(ex) => callback(ex).asLeft
      case Failure(ex)                             => returnLeftError(ex)
    }

  }

}
