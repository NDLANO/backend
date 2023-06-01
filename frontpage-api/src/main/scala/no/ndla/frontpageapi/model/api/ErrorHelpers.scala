/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.effect.IO
import no.ndla.common.Clock
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, NotFoundException, ValidationException}
import no.ndla.network.logging.FLogging
import no.ndla.network.tapir.{ErrorBody, TapirErrorHelpers}

trait ErrorHelpers extends TapirErrorHelpers with FLogging {
  this: Props with Clock =>

  import ErrorHelpers._

  override def returnError(ex: Throwable): IO[ErrorBody] = {
    ex match {
      case a: ValidationException        => IO(badRequest(ex.getMessage))
      case ex: NotFoundException         => IO(notFoundWithMsg(ex.getMessage))
      case ex: LanguageNotFoundException => IO(notFoundWithMsg(ex.getMessage))
      case ex                            => logger.error(ex)(s"Internal error: ${ex.getMessage}").as(generic)
    }
  }

}
