/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the editorial notes")
case class EditorNote(
    @(ApiModelProperty @field)(description = "Editorial note") note: String,
    @(ApiModelProperty @field)(description = "User which saved the note") user: String,
    @(ApiModelProperty @field)(description = "Status of article at saved time") status: Status,
    @(ApiModelProperty @field)(description = "Timestamp of when note was saved") timestamp: NDLADate
)

object EditorNote {
  implicit def encoder: Encoder[EditorNote] = deriveEncoder
  implicit def decoder: Decoder[EditorNote] = deriveDecoder
}
