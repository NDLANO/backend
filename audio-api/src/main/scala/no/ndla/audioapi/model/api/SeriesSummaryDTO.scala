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

@description("Short summary of information about the series")
case class SeriesSummaryDTO(
    @description("The unique id of the series") id: Long,
    @description("The title of the series") title: TitleDTO,
    @description("The description of the series") description: DescriptionDTO,
    @description("A list of available languages for this series") supportedLanguages: Seq[String],
    @description("A list of episode summaries") episodes: Option[Seq[AudioSummaryDTO]],
    @description("Cover photo for the series") coverPhoto: CoverPhotoDTO
)

object SeriesSummaryDTO {
  implicit val SeriesSummaryTSI: TSIType[SeriesSummaryDTO] = {
    @unused
    implicit val audioSummaryReference: TSType[Option[Seq[AudioSummaryDTO]]] =
      TSType.external[Option[Seq[AudioSummaryDTO]]]("IAudioSummary")
    TSType.fromCaseClass[SeriesSummaryDTO]
  }
}
