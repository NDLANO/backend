/*
 * Part of NDLA article-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class SearchSettings(
    query: Option[String],
    withIdIn: List[Long],
    language: String,
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    articleTypes: Seq[String],
    fallback: Boolean,
    grepCodes: Seq[String],
    shouldScroll: Boolean,
    availability: Seq[Availability.Value]
)
