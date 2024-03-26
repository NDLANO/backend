/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class SubjectPageId(id: Long)

object SubjectPageId {
  implicit def encoder: Encoder.AsObject[SubjectPageId] = deriveEncoder[SubjectPageId]

  implicit def decoder: Decoder[SubjectPageId] = deriveDecoder[SubjectPageId]
}
