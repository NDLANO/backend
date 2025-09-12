/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException, TaxonomyException}
import no.ndla.network.tapir.{AllErrors, ErrorHandling, ErrorHelpers, ValidationErrorBody}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.searchapi.model.api.{InvalidIndexBodyException, ResultWindowTooLargeException}
import cats.implicits.*
import no.ndla.search.model.domain.DocumentConflictException

class ControllerErrorHandling(using errorHelpers: ErrorHelpers, clock: Clock) extends ErrorHandling {
  import errorHelpers.*

  val TAXONOMY_FAILURE         = "TAXONOMY_FAILURE"
  val INVALID_BODY             = "INVALID_BODY"
  val INVALID_BODY_DESCRIPTION =
    "Unable to index the requested document because body was invalid."

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case rw: ResultWindowTooLargeException => errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: IndexNotFoundException         => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 503)
    case iibe: InvalidIndexBodyException   => errorBody(INVALID_BODY, iibe.getMessage, 400)
    case te: TaxonomyException             => errorBody(TAXONOMY_FAILURE, te.getMessage, 500)
    case v: ValidationException            =>
      ValidationErrorBody(VALIDATION, VALIDATION_DESCRIPTION, clock.now(), messages = v.errors.some, 400)
    case ade: AccessDeniedException   => forbiddenMsg(ade.getMessage)
    case _: DocumentConflictException => indexConflict
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      invalidSearchContext
  }

}
