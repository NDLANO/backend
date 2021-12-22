/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of a visual element")
case class VisualElement(
    @(ApiModelProperty @field)(description =
      "Html containing the visual element. May contain any legal html element, including the embed-tag") visualElement: String,
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which article translation this visual element belongs to") language: String)
    extends LanguageField
