/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.effect.IO
import no.ndla.common.Clock
import no.ndla.common.errors.NotFoundException
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.domain.Errors.{
  LanguageNotFoundException,
  SubjectPageNotFoundException,
  ValidationException
}
import no.ndla.network.logging.FLogging
import no.ndla.network.tapir.{ErrorBody, TapirErrorHelpers}

trait ErrorHelpers extends TapirErrorHelpers with FLogging {
  this: Props with Clock =>

  import ErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, IO[ErrorBody]] = {
    case ex: ValidationException          => IO(badRequest(ex.getMessage))
    case ex: SubjectPageNotFoundException => IO(notFoundWithMsg(ex.getMessage))
    case ex: NotFoundException            => IO(notFoundWithMsg(ex.getMessage))
    case ex: LanguageNotFoundException    => IO(notFoundWithMsg(ex.getMessage))
  }

}
