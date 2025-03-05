/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class AboutSubjectDTO(
    title: String,
    description: String,
    visualElement: VisualElementDTO
)

object AboutSubjectDTO {
  implicit val encoder: Encoder[AboutSubjectDTO] = deriveEncoder
  implicit val decoder: Decoder[AboutSubjectDTO] = deriveDecoder
}
