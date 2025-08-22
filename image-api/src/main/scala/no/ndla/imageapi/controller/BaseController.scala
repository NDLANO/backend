package no.ndla.imageapi.controller

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, ValidationException}
import no.ndla.database.DataSource
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.{
  ImageNotFoundException,
  ImageStorageException,
  ImportException,
  InvalidUrlException,
  ResultWindowTooLargeException
}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{AllErrors, ErrorHelpers, TapirController}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException

abstract class BaseController(using
    props: Props,
    clock: Clock,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    dataSource: DataSource
) extends TapirController {
  import errorHelpers.*
  import ImageErrorHelpers.*
  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case v: ValidationException    => validationError(v)
    case a: AccessDeniedException  => forbiddenMsg(a.getMessage)
    case _: IndexNotFoundException => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 500)
    case i: ImageNotFoundException => notFoundWithMsg(i.getMessage)
    case b: ImportException        => errorBody(IMPORT_FAILED, b.getMessage, 422)
    case iu: InvalidUrlException   => errorBody(INVALID_URL, iu.getMessage, 400)
    case s: ImageStorageException  =>
      errorBody(GATEWAY_TIMEOUT, s.getMessage, 504)
    case rw: ResultWindowTooLargeException =>
      errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: PSQLException =>
      dataSource.connectToDatabase()
      errorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, 500)
    case NdlaSearchException(_, Some(rf), _, _)
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
