/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.DeriveHelpers
import sttp.tapir.Schema.annotations.description
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema

@description("Information about the editorial notes")
case class RevisionMetaDTO(
    @description("An unique uuid of the revision. If none supplied, one is generated.")
    id: Option[String],
    @description("A date on which the article would need to be revised")
    revisionDate: NDLADate,
    @description("Notes to keep track of what needs to happen before revision")
    note: String,
    @description("Status of a revision, either 'revised' or 'needs-revision'")
    status: String,
)

object RevisionMetaDTO {
  implicit def encoder: Encoder[RevisionMetaDTO] = deriveEncoder[RevisionMetaDTO]
  implicit def decoder: Decoder[RevisionMetaDTO] = deriveDecoder[RevisionMetaDTO]
  implicit def schema: Schema[RevisionMetaDTO]   = DeriveHelpers.getSchema[RevisionMetaDTO]
}
