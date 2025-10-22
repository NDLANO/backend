/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.DeriveHelpers

@description("Short summary of information about the series")
case class SeriesSummaryDTO(
    @description("The unique id of the series")
    id: Long,
    @description("The title of the series")
    title: TitleDTO,
    @description("The description of the series")
    description: DescriptionDTO,
    @description("A list of available languages for this series")
    supportedLanguages: Seq[String],
    @description("A list of episode summaries")
    episodes: Option[Seq[AudioSummaryDTO]],
    @description("Cover photo for the series")
    coverPhoto: CoverPhotoDTO,
)
object SeriesSummaryDTO {
  implicit val encoder: Encoder[SeriesSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[SeriesSummaryDTO] = deriveDecoder

  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[SeriesSummaryDTO] = DeriveHelpers.getSchema
}
