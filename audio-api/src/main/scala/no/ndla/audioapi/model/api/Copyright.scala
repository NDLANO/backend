/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import java.time.LocalDateTime
import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class Copyright(
    @(ApiModelProperty @field)(description = "The license for the audio") license: License,
    @(ApiModelProperty @field)(description = "Reference to where the audio is procured") origin: Option[String],
    @(ApiModelProperty @field)(description = "List of creators") creators: Seq[Author] = Seq.empty,
    @(ApiModelProperty @field)(description = "List of processors") processors: Seq[Author] = Seq.empty,
    @(ApiModelProperty @field)(description = "List of rightsholders") rightsholders: Seq[Author] = Seq.empty,
    @(ApiModelProperty @field)(description = "Reference to a agreement id") agreementId: Option[Long],
    @(ApiModelProperty @field)(description = "Date from which the copyright is valid") validFrom: Option[LocalDateTime],
    @(ApiModelProperty @field)(description = "Date to which the copyright is valid") validTo: Option[LocalDateTime]
)
