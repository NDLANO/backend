/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.NotFoundException
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.domain.Errors.{
  LanguageNotFoundException,
  SubjectPageNotFoundException,
  ValidationException
}
import no.ndla.network.tapir.{ErrorBody, TapirErrorHelpers}

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock =>

  import ErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, ErrorBody] = {
    case ex: ValidationException          => badRequest(ex.getMessage)
    case ex: SubjectPageNotFoundException => notFoundWithMsg(ex.getMessage)
    case ex: NotFoundException            => notFoundWithMsg(ex.getMessage)
    case ex: LanguageNotFoundException    => notFoundWithMsg(ex.getMessage)
  }

}
