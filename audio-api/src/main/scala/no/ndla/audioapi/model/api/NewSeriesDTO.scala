/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Meta information about podcast series")
case class NewSeriesDTO(
    @description("Header for the series") title: String,
    @description("Description for the series") description: String,
    @description("Cover photo for the series") coverPhotoId: String,
    @description("Cover photo alttext for the series") coverPhotoAltText: String,
    @description("Ids for episodes of the series") episodes: Set[Long],
    @description("ISO 639-1 code that represents the language used in this resource") language: String,
    @description("Revision number of this series (Only used to do locking when updating)") revision: Option[Int],
    @description("Specifies if this series generates rss-feed") hasRSS: Option[Boolean]
)
