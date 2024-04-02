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
case class SeriesSummary(
    @description("The unique id of the series") id: Long,
    @description("The title of the series") title: Title,
    @description("The description of the series") description: Description,
    @description("A list of available languages for this series") supportedLanguages: Seq[String],
    @description("A list of episode summaries") episodes: Option[Seq[AudioSummary]],
    @description("Cover photo for the series") coverPhoto: CoverPhoto
)

object SeriesSummary {
  implicit val SeriesSummaryTSI: TSIType[SeriesSummary] = {
    @unused
    implicit val audioSummaryReference: TSType[Option[Seq[AudioSummary]]] =
      TSType.external[Option[Seq[AudioSummary]]]("IAudioSummary")
    TSType.fromCaseClass[SeriesSummary]
  }
}
