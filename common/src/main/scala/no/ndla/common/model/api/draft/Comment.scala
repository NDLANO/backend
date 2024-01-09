/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api.draft

import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about a comment attached to an article")
case class Comment(
    @(ApiModelProperty @field)(description = "Id of the comment") id: String,
    @(ApiModelProperty @field)(description = "Content of the comment") content: String,
    @(ApiModelProperty @field)(description = "When the comment was created") created: NDLADate,
    @(ApiModelProperty @field)(description = "When the comment was last updated") updated: NDLADate,
    @(ApiModelProperty @field)(description = "If the comment is open or closed") isOpen: Boolean,
    @(ApiModelProperty @field)(description = "If the comment is solved or not") solved: Boolean
)
