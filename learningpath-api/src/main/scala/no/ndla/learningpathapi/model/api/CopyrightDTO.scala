/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.{AuthorDTO, LicenseDTO}
import sttp.tapir.Schema.annotations.description

@description("Description of copyright information")
case class CopyrightDTO(
    @description("Describes the license of the learningpath") license: LicenseDTO,
    @description("List of authors") contributors: Seq[AuthorDTO]
)

object CopyrightDTO {
  implicit val encoder: Encoder[CopyrightDTO] = deriveEncoder
  implicit val decoder: Decoder[CopyrightDTO] = deriveDecoder
}
