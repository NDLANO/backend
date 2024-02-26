/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about the editorial notes")
case class RevisionMeta(
    @description("An unique uuid of the revision. If none supplied, one is generated.") id: Option[String],
    @description("A date on which the article would need to be revised") revisionDate: NDLADate,
    @description("Notes to keep track of what needs to happen before revision") note: String,
    @description("Status of a revision, either 'revised' or 'needs-revision'") status: String
)

object RevisionMeta {
    implicit def encoder: Encoder[RevisionMeta] = deriveEncoder[RevisionMeta]
    implicit def decoder: Decoder[RevisionMeta] = deriveDecoder[RevisionMeta]
}
