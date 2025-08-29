/*
 * Part of NDLA network
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.StrictLogging

trait ErrorHandling(using errorHelpers: ErrorHelpers) extends StrictLogging {
  def logError(e: Throwable): Unit = {
    logger.error(e.getMessage, e)
  }

  private def handleUnknownError(e: Throwable): ErrorBody = {
    logError(e)
    errorHelpers.generic
  }

  def handleErrors: PartialFunction[Throwable, AllErrors]
  def returnError(ex: Throwable): AllErrors                   = handleErrors.applyOrElse(ex, handleUnknownError)
  def returnLeftError[R](ex: Throwable): Either[AllErrors, R] = returnError(ex).asLeft[R]
}
