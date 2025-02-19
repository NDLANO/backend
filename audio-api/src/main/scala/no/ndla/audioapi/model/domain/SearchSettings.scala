/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import no.ndla.audioapi.model.Sort

case class SearchSettings(
    query: Option[String],
    language: Option[String],
    license: Option[String],
    page: Option[Int],
    pageSize: Option[Int],
    sort: Sort,
    shouldScroll: Boolean,
    audioType: Option[AudioType.Value],
    seriesFilter: Option[Boolean],
    fallback: Boolean
)
