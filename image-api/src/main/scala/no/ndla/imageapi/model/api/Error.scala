/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, ValidationException}
import no.ndla.common.Clock
import no.ndla.imageapi.Props
import no.ndla.imageapi.integration.DataSource
import no.ndla.imageapi.model._
import no.ndla.network.tapir.{AllErrors, TapirErrorHelpers}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._
  import ImageErrorHelpers._
  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case v: ValidationException    => validationError(v)
    case a: AccessDeniedException  => forbiddenMsg(a.getMessage)
    case _: IndexNotFoundException => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 500)
    case i: ImageNotFoundException => notFoundWithMsg(i.getMessage)
    case b: ImportException        => errorBody(IMPORT_FAILED, b.getMessage, 422)
    case iu: InvalidUrlException   => errorBody(INVALID_URL, iu.getMessage, 400)
    case s: ImageStorageException =>
      errorBody(GATEWAY_TIMEOUT, s.getMessage, 504)
    case rw: ResultWindowTooLargeException =>
      errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: PSQLException =>
      DataSource.connectToDatabase()
      errorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, 500)
    case NdlaSearchException(_, Some(rf), _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      errorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, 400)
    case _: FileTooBigException => errorBody(FILE_TOO_BIG, fileTooBigError, 413)
  }

  object ImageErrorHelpers {
    val fileTooBigError: String =
      s"The file is too big. Max file size is ${props.MaxImageFileSizeBytes / 1024 / 1024} MiB"
    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
  }
}
