/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.common.model.api.{Author, License}
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class Copyright(
    @(ApiModelProperty @field)(description = "Describes the license of the learningpath") license: License,
    @(ApiModelProperty @field)(description = "List of authors") contributors: Seq[Author]
)
