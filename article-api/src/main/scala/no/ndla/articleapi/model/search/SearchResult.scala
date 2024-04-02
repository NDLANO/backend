/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.search

case class SearchResult[T](
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[T],
    scrollId: Option[String]
)
