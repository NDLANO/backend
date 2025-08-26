/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

trait TapirErrorHandling(using errorHandling: ErrorHandling) extends StrictLogging {

  import errorHandling.*

  def handleErrors: PartialFunction[Throwable, AllErrors]      = errorHandling.handleErrors
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
