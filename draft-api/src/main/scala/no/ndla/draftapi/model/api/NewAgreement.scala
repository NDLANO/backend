/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Information about the agreement")
case class NewAgreement(
    @(ApiModelProperty @field)(description = "Titles for the agreement") title: String,
    @(ApiModelProperty @field)(description = "The content of the agreement") content: String,
    @(ApiModelProperty @field)(description = "Describes the copyright information for the article") copyright: NewAgreementCopyright)
