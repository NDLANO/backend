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
case class UpdatedAgreement(
    @(ApiModelProperty @field)(description = "The title of the agreement") title: Option[String],
    @(ApiModelProperty @field)(description = "The content of the agreement") content: Option[String],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the agreement") copyright: Option[
      NewAgreementCopyright]
)
