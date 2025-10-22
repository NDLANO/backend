/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.*
import no.ndla.database.DataSource
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.{
  ImageNotFoundException,
  ImageStorageException,
  ImportException,
  InvalidUrlException,
  ResultWindowTooLargeException,
}
import no.ndla.network.tapir.{AllErrors, ErrorHandling, ErrorHelpers}
import no.ndla.search.NdlaSearchException
import no.ndla.search.IndexNotFoundException
import org.postgresql.util.PSQLException

class ControllerErrorHandling(using props: Props, dataSource: => DataSource, errorHelpers: ErrorHelpers)
    extends ErrorHandling {
  import errorHelpers.*
  import no.ndla.imageapi.model.ImageErrorHelpers.*

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case v: ValidationException            => validationError(v)
    case a: AccessDeniedException          => forbiddenMsg(a.getMessage)
    case _: IndexNotFoundException         => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 500)
    case i: ImageNotFoundException         => notFoundWithMsg(i.getMessage)
    case b: ImportException                => errorBody(IMPORT_FAILED, b.getMessage, 422)
    case iu: InvalidUrlException           => errorBody(INVALID_URL, iu.getMessage, 400)
    case s: ImageStorageException          => errorBody(GATEWAY_TIMEOUT, s.getMessage, 504)
    case rw: ResultWindowTooLargeException => errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: PSQLException                  =>
      dataSource.connectToDatabase()
      errorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, 500)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf
          .error
          .rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      errorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, 400)
    case _: FileTooBigException => errorBody(FILE_TOO_BIG, fileTooBigError, 413)
  }
}
