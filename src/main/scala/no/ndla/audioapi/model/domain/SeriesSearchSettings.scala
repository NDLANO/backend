package no.ndla.audioapi.model.domain

import no.ndla.audioapi.model.Sort

case class SeriesSearchSettings(
    query: Option[String],
    language: Option[String],
    page: Option[Int],
    pageSize: Option[Int],
    sort: Sort.Value,
    shouldScroll: Boolean,
)
