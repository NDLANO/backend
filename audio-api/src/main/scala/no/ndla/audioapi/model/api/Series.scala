/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import com.scalatsi._
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

@description("Meta information about the series")
case class Series(
    @description("The unique id of this series") id: Long,
    @description("The revision number of this series") revision: Int,
    @description("The title of the series") title: Title,
    @description("The description of the series") description: Description,
    @description("Cover photo for the series") coverPhoto: CoverPhoto,
    @description("The metainfo of the episodes in the series") episodes: Option[Seq[AudioMetaInformation]],
    @description("A list of available languages for this series") supportedLanguages: Seq[String],
    @description("Specifies if this series generates rss-feed") hasRSS: Boolean
)

object Series {
  implicit val seriesTSI: TSIType[Series] = {
    @unused
    implicit val audioMetaInformationReference: TSNamedType[AudioMetaInformation] =
      TSType.external[AudioMetaInformation]("IAudioMetaInformation")
    TSType.fromCaseClass[Series]
  }
}
