/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import com.scalatsi.TSType
import com.scalatsi.TypescriptType.TSNull
import no.ndla.common.model.api.Deletable
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information for the image")
case class UpdateImageMetaInformation(
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language") language: String,
    @(ApiModelProperty @field)(description = "Title for the image") title: Option[String],
    @(ApiModelProperty @field)(description = "Alternative text for the image") alttext: Deletable[String],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the image") copyright: Option[Copyright],
    @(ApiModelProperty @field)(description = "Searchable tags for the image") tags: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "Caption for the image") caption: Option[String],
    @(ApiModelProperty @field)(description = "Describes if the model has released use of the image", allowableValues = "yes,no,not-applicable") modelReleased: Option[String]
)
// format: on

object UpdateImageMetaInformation {
  // This alias is required since scala-tsi doesn't understand that Null is `null`
  // See: https://github.com/scala-tsi/scala-tsi/issues/172
  implicit val nullTsType: TSType[Null] = TSType(TSNull)
}
