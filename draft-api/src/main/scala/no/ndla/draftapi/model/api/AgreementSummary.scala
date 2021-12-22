/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the agreement")
case class AgreementSummary(@(ApiModelProperty @field)(description = "The unique id of the agreement") id: Long,
                            @(ApiModelProperty @field)(description = "The title of the agreement") title: String,
                            @(ApiModelProperty @field)(description = "The license of the agreement") license: String)
