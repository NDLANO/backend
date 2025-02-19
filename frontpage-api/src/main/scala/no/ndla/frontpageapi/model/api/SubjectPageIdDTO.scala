/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class SubjectPageIdDTO(id: Long)

object SubjectPageIdDTO {
  implicit def encoder: Encoder.AsObject[SubjectPageIdDTO] = deriveEncoder[SubjectPageIdDTO]

  implicit def decoder: Decoder[SubjectPageIdDTO] = deriveDecoder[SubjectPageIdDTO]
}
