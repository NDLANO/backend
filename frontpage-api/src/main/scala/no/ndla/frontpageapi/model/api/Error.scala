/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.NotFoundException

import java.time.LocalDateTime
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import org.log4s.{Logger, getLogger}

sealed trait Error {
  val code: String
  val description: String
  val occuredAt: LocalDateTime
}
case class NotFoundError(code: String, description: String, occuredAt: LocalDateTime)            extends Error
case class GenericError(code: String, description: String, occuredAt: LocalDateTime)             extends Error
case class BadRequestError(code: String, description: String, occuredAt: LocalDateTime)          extends Error
case class UnprocessableEntityError(code: String, description: String, occuredAt: LocalDateTime) extends Error
case class UnauthorizedError(code: String, description: String, occuredAt: LocalDateTime)        extends Error
case class ForbiddenError(code: String, description: String, occuredAt: LocalDateTime)           extends Error

trait ErrorHelpers {
  this: Props with Clock =>

  val logger: Logger = getLogger

  object ErrorHelpers {
    val GENERIC              = "GENERIC"
    val NOT_FOUND            = "NOT_FOUND"
    val BAD_REQUEST          = "BAD_REQUEST"
    val UNPROCESSABLE_ENTITY = "UNPROCESSABLE_ENTITY"
    val UNAUTHORIZED         = "UNAUTHORIZED"
    val FORBIDDEN            = "FORBIDDEN"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    val NOT_FOUND_DESCRIPTION = s"The page you requested does not exist"

    val UNAUTHORIZED_DESCRIPTION = "Missing user/client-id or role"
    val FORBIDDEN_DESCRIPTION    = "You do not have the required permissions to access that resource"

    def generic: GenericError                       = GenericError(GENERIC, GENERIC_DESCRIPTION, clock.now())
    def notFound: NotFoundError                     = NotFoundError(NOT_FOUND, NOT_FOUND_DESCRIPTION, clock.now())
    def notFoundWithMsg(msg: String): NotFoundError = NotFoundError(NOT_FOUND, msg, clock.now())
    def badRequest(msg: String): BadRequestError    = BadRequestError(BAD_REQUEST, msg, clock.now())
    def unprocessableEntity(msg: String): UnprocessableEntityError =
      UnprocessableEntityError(UNPROCESSABLE_ENTITY, msg, clock.now())
    def unauthorized: UnauthorizedError = UnauthorizedError(UNAUTHORIZED, UNAUTHORIZED_DESCRIPTION, clock.now())
    def forbidden: ForbiddenError       = ForbiddenError(FORBIDDEN, FORBIDDEN_DESCRIPTION, clock.now())

    def returnError(ex: Throwable): Error = {
      ex match {
        case v: ValidationException => unprocessableEntity(v.getMessage)
        case n: NotFoundException   => notFoundWithMsg(n.getMessage)
        case _ =>
          logger.error(ex)(ex.toString)
          generic
      }
    }
  }
}
