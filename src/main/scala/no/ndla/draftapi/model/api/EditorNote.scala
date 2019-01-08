/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import java.util.Date

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the editorial notes")
case class EditorNote(@(ApiModelProperty @field)(description = "Editorial notes") notes: Seq[String],
                      @(ApiModelProperty @field)(description = "User which saved the notes") user: String,
                      @(ApiModelProperty @field)(description = "Status of article at saved time") status: Status,
                      @(ApiModelProperty @field)(description = "Timestamp of when article was saved") timestamp: Date)
