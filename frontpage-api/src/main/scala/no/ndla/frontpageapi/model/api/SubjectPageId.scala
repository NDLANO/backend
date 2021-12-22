/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.Applicative
import cats.effect.Sync
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import scala.language.higherKinds
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe._

case class SubjectPageId(id: Long)

object SubjectPageId {
  implicit def encoder[F[_]: Applicative]: EntityEncoder[F, SubjectPageId] =
    jsonEncoderOf[F, SubjectPageId](deriveEncoder[SubjectPageId])

  implicit def decoder[F[_]: Sync]: EntityDecoder[F, SubjectPageId] =
    jsonOf[F, SubjectPageId](Sync[F], deriveDecoder[SubjectPageId])
}
