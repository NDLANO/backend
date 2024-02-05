/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class DraftCopyright(
    @(ApiModelProperty @field)(description = "Describes the license of the article") license: Option[License],
    @(ApiModelProperty @field)(description = "Reference to where the article is procured") origin: Option[String],
    @(ApiModelProperty @field)(description = "List of creators") creators: Seq[Author],
    @(ApiModelProperty @field)(description = "List of processors") processors: Seq[Author],
    @(ApiModelProperty @field)(description = "List of rightsholders") rightsholders: Seq[Author],
    @(ApiModelProperty @field)(description = "Date from which the copyright is valid") validFrom: Option[NDLADate],
    @(ApiModelProperty @field)(description = "Date to which the copyright is valid") validTo: Option[NDLADate],
    @(ApiModelProperty @field)(description = "Whether or not the content has been processed") processed: Boolean
)

object DraftCopyright {
    implicit def encoder: Encoder[DraftCopyright] = deriveEncoder[DraftCopyright]
    implicit def decoder: Decoder[DraftCopyright] = deriveDecoder[DraftCopyright]
}
