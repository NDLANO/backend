/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import io.circe.{Encoder, Decoder}

case class NewOrUpdatedAboutSubjectDTO(
    title: String,
    description: String,
    language: String,
    visualElement: NewOrUpdatedVisualElementDTO,
)
object NewOrUpdatedAboutSubjectDTO {

  implicit val encoder: Encoder[NewOrUpdatedAboutSubjectDTO] = deriveEncoder
  implicit val decoder: Decoder[NewOrUpdatedAboutSubjectDTO] = deriveDecoder
}
