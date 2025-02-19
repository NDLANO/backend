/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.domain

import no.ndla.search.model.domain.TermAggregation

case class SearchResult[T](
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[T],
    aggregations: Seq[TermAggregation],
    scrollId: Option[String]
)
