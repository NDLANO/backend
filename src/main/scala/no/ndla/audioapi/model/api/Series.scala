/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import com.scalatsi.{TSIType, TSType}
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information about the series")
case class Series(
    @(ApiModelProperty @field)(description = "The unique id of this series") id: Long,
    @(ApiModelProperty @field)(description = "The revision number of this series") revision: Int,
    @(ApiModelProperty @field)(description = "The title of the series") title: Title,
    @(ApiModelProperty @field)(description = "The description of the series") description: Description,
    @(ApiModelProperty @field)(description = "Cover photo for the series") coverPhoto: CoverPhoto,
    @(ApiModelProperty @field)(description = "The metainfo of the episodes in the series") episodes: Option[Seq[AudioMetaInformation]],
    @(ApiModelProperty @field)(description = "A list of available languages for this series") supportedLanguages: Seq[String]
)
// format: on
object Series {
  implicit val seriesTSI: TSIType[Series] = {
    implicit val audioMetaInformationReference: TSType[AudioMetaInformation] = TSType.external[AudioMetaInformation]("IAudioMetaInformation")
    TSType.fromCaseClass[Series]
  }
}