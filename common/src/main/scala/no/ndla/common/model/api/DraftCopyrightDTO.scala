/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

@description("Description of copyright information")
case class DraftCopyrightDTO(
    @description("Describes the license of the article") license: Option[LicenseDTO],
    @description("Reference to where the article is procured") origin: Option[String],
    @description("List of creators") creators: Seq[AuthorDTO],
    @description("List of processors") processors: Seq[AuthorDTO],
    @description("List of rightsholders") rightsholders: Seq[AuthorDTO],
    @description("Date from which the copyright is valid") validFrom: Option[NDLADate],
    @description("Date to which the copyright is valid") validTo: Option[NDLADate],
    @description("Whether or not the content has been processed") processed: Boolean
)

object DraftCopyrightDTO {
  implicit def encoder: Encoder[DraftCopyrightDTO] = deriveEncoder[DraftCopyrightDTO]
  implicit def decoder: Decoder[DraftCopyrightDTO] = deriveDecoder[DraftCopyrightDTO]
  implicit def schema: Schema[DraftCopyrightDTO]   = Schema.derived[DraftCopyrightDTO]
}
