/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the subject")
case class Subject(@(ApiModelProperty @field)(description = "The name of the subject") name: String,
                   @(ApiModelProperty @field)(description = "The path to the article") path: String,
                   @(ApiModelProperty @field)(description = "List of breadcrumbs to article") breadcrumbs: Seq[String])
