/*
 * Part of NDLA draft-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import java.time.LocalDateTime
import scala.annotation.meta.field

@ApiModel(description = "Information about a comment attached to an article")
case class Comment(
    @(ApiModelProperty @field)(description = "Id of the comment") id: String,
    @(ApiModelProperty @field)(description = "Content of the comment") content: String,
    @(ApiModelProperty @field)(description = "When the comment was created") created: LocalDateTime,
    @(ApiModelProperty @field)(description = "When the comment was last updated") updated: LocalDateTime,
    @(ApiModelProperty @field)(description = "If the comment is open or closed") isOpen: Boolean
)
