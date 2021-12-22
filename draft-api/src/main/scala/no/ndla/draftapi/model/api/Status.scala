/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class Status(
    @(ApiModelProperty @field)(description = "The current status of the article") current: String,
    @(ApiModelProperty @field)(description = "Previous statuses this article has been in") other: Seq[String])
