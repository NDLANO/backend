package no.ndla.audioapi.model.domain

import no.ndla.audioapi.model.Sort

case class SearchSettings(
    query: Option[String],
    language: Option[String],
    license: Option[String],
    page: Option[Int],
    pageSize: Option[Int],
    sort: Sort.Value,
    shouldScroll: Boolean,
    audioType: Option[AudioType.Value],
    seriesFilter: Option[Boolean]
)
