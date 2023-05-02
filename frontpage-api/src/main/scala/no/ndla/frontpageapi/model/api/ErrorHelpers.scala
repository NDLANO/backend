/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import no.ndla.common.Clock
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, NotFoundException, ValidationException}
import no.ndla.network.tapir.{ErrorBody, TapirErrorHelpers}
import org.log4s.{Logger, getLogger}

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock =>

  import ErrorHelpers._

  val logger: Logger = getLogger

  override def returnError(ex: Throwable): ErrorBody = {
    ex match {
      case a: ValidationException        => badRequest(ex.getMessage)
      case ex: NotFoundException         => notFoundWithMsg(ex.getMessage)
      case ex: LanguageNotFoundException => notFoundWithMsg(ex.getMessage)
      case ex =>
        logger.error(ex)(s"Internal error: ${ex.getMessage}")
        generic
    }
  }

}
