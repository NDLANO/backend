/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about an author")
case class Author(
  @(ApiModelProperty @field)(
    description = "The description of the author. Eg. Photographer or Supplier",
    allowableValues = "originator,photographer,artist,editorial,writer,scriptwriter,reader,translator,director,illustrator,cowriter,composer,processor,facilitator,editorial,linguistic,idea,compiler,correction,rightsholder,publisher,distributor,supplier"
  )
  `type`: String,
  @(ApiModelProperty @field)(description = "The name of the of the author") name: String
)
// format: on
