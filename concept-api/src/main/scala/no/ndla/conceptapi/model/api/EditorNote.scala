/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the editorial notes")
case class EditorNote(
    @(ApiModelProperty @field)(description = "Editorial note") note: String,
    @(ApiModelProperty @field)(description = "User which saved the note") updatedBy: String,
    @(ApiModelProperty @field)(description = "Status of concept at saved time") status: Status,
    @(ApiModelProperty @field)(description = "Timestamp of when note was saved") timestamp: NDLADate
)

object EditorNote {
  implicit val encoder: Encoder[EditorNote] = deriveEncoder
  implicit val decoder: Decoder[EditorNote] = deriveDecoder
}
