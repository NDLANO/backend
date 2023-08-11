/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class Copyright(
    @(ApiModelProperty @field)(description = "Describes the license of the image") license: License,
    @(ApiModelProperty @field)(description = "Reference to where the image is procured") origin: String,
    @(ApiModelProperty @field)(description = "List of creators") creators: Seq[Author],
    @(ApiModelProperty @field)(description = "List of processors") processors: Seq[Author],
    @(ApiModelProperty @field)(description = "List of rightsholders") rightsholders: Seq[Author],
    @(ApiModelProperty @field)(description = "Reference to a agreement id") agreementId: Option[Long],
    @(ApiModelProperty @field)(description = "The date from which the license is valid") validFrom: Option[
      NDLADate
    ],
    @(ApiModelProperty @field)(description = "The date to which the license is valid") validTo: Option[NDLADate]
)
