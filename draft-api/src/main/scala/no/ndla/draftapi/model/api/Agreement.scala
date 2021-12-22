/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the agreement")
case class Agreement(
    @(ApiModelProperty @field)(description = "The unique id of the article") id: Long,
    @(ApiModelProperty @field)(description = "Titles for the agreement") title: String,
    @(ApiModelProperty @field)(description = "The content of the agreement") content: String,
    @(ApiModelProperty @field)(description = "Describes the copyright information for the agreement") copyright: Copyright,
    @(ApiModelProperty @field)(description = "When the agreement was created") created: Date,
    @(ApiModelProperty @field)(description = "When the agreement was last updated") updated: Date,
    @(ApiModelProperty @field)(description = "By whom the agreement was last updated") updatedBy: String)
