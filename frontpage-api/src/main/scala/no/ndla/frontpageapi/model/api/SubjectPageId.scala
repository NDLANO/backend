/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.Applicative
import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import io.circe.generic.semiauto._

case class SubjectPageId(id: Long)

object SubjectPageId {
  implicit def encoder: Encoder.AsObject[SubjectPageId] = deriveEncoder[SubjectPageId]

  implicit def decoder: Decoder[SubjectPageId] = deriveDecoder[SubjectPageId]
}
