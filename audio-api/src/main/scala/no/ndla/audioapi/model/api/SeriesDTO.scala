/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Meta information about the series")
case class SeriesDTO(
    @description("The unique id of this series") id: Long,
    @description("The revision number of this series") revision: Int,
    @description("The title of the series") title: TitleDTO,
    @description("The description of the series") description: DescriptionDTO,
    @description("Cover photo for the series") coverPhoto: CoverPhotoDTO,
    @description("The metainfo of the episodes in the series") episodes: Option[Seq[AudioMetaInformationDTO]],
    @description("A list of available languages for this series") supportedLanguages: Seq[String],
    @description("Specifies if this series generates rss-feed") hasRSS: Boolean
)
