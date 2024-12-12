/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the editorial notes")
case class RevisionMetaDTO(
    @description("A date on which the article would need to be revised") revisionDate: NDLADate,
    @description("Notes to keep track of what needs to happen before revision") note: String,
    @description("Status of a revision, either 'revised' or 'needs-revision'") status: String
)

object RevisionMetaDTO {
  implicit val encoder: Encoder[RevisionMetaDTO] = deriveEncoder
  implicit val decoder: Decoder[RevisionMetaDTO] = deriveDecoder
}
