/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.myndlaapi.Props
import no.ndla.myndlaapi.integration.DataSource
import no.ndla.myndlaapi.model.arena.domain.TopicGoneException
import no.ndla.myndlaapi.model.domain.InvalidStatusException
import no.ndla.network.tapir.{AllErrors, ErrorBody, TapirErrorHelpers, ValidationErrorBody}
import org.postgresql.util.PSQLException

trait ErrorHelpers extends TapirErrorHelpers with StrictLogging {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case tge: TopicGoneException =>
      ErrorBody(NOT_FOUND, tge.getMessage, clock.now(), 410)
    case nfe: NotFoundException =>
      ErrorBody(NOT_FOUND, nfe.getMessage, clock.now(), 404)
    case a: AccessDeniedException if a.unauthorized =>
      ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 401)
    case a: AccessDeniedException =>
      ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case mse: InvalidStatusException =>
      ErrorBody(MISSING_STATUS, mse.getMessage, clock.now(), 400)
    case _: PSQLException =>
      DataSource.connectToDatabase()
      ErrorHelpers.generic
    case v: ValidationException =>
      ValidationErrorBody(VALIDATION, VALIDATION_DESCRIPTION, clock.now(), Some(v.errors), 400)
  }
}
