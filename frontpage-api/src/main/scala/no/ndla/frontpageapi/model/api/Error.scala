/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import java.time.LocalDateTime
import no.ndla.frontpageapi.FrontpageApiProperties
import cats.Applicative
import cats.effect.Sync
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import scala.language.higherKinds
import io.circe.generic.semiauto._

case class Error(code: String, description: String, occuredAt: LocalDateTime)

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val BAD_REQUEST = "BAD_REQUEST"
  val UNPROCESSABLE_ENTITY = "UNPROCESSABLE_ENTITY"
  val UNAUTHORIZED = "UNAUTHORIZED"
  val FORBIDDEN = "FORBIDDEN"

  val GENERIC_DESCRIPTION =
    s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${FrontpageApiProperties.ContactEmail} if the error persists."
  val NOT_FOUND_DESCRIPTION = s"The page you requested does not exist"

  val UNAUTHORIZED_DESCRIPTION = "Missing user/client-id or role"
  val FORBIDDEN_DESCRIPTION = "You do not have the required permissions to access that resource"

  def generic: Error = Error(GENERIC, GENERIC_DESCRIPTION, LocalDateTime.now)
  def notFound: Error = Error(NOT_FOUND, NOT_FOUND_DESCRIPTION, LocalDateTime.now)
  def badRequest(msg: String): Error = Error(BAD_REQUEST, msg, LocalDateTime.now)
  def unprocessableEntity(msg: String): Error = Error(UNPROCESSABLE_ENTITY, msg, LocalDateTime.now)
  def unauthorized: Error = Error(UNAUTHORIZED, UNAUTHORIZED_DESCRIPTION, LocalDateTime.now)
  def forbidden: Error = Error(FORBIDDEN, FORBIDDEN_DESCRIPTION, LocalDateTime.now)

  implicit def encoder[F[_]: Applicative]: EntityEncoder[F, Error] =
    jsonEncoderOf[F, Error](deriveEncoder[Error])

  implicit def decoder[F[_]: Sync]: EntityDecoder[F, Error] =
    jsonOf[F, Error](Sync[F], deriveDecoder[Error])
}
