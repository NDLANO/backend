package no.ndla.frontpageapi.controller

import no.ndla.common.Clock
import no.ndla.common.configuration.BaseProps
import no.ndla.common.errors.{NotFoundException, ValidationException}
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, SubjectPageNotFoundException}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{AllErrors, ErrorHelpers, TapirController}

abstract class BaseController(using
    props: BaseProps,
    clock: Clock,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers
) extends TapirController {
  import errorHelpers.*

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case ex: ValidationException          => badRequest(ex.getMessage)
    case ex: SubjectPageNotFoundException => notFoundWithMsg(ex.getMessage)
    case ex: NotFoundException            => notFoundWithMsg(ex.getMessage)
    case ex: LanguageNotFoundException    => notFoundWithMsg(ex.getMessage)
  }
}
