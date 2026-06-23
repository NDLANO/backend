/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "SearchResult")
data class SearchResultDTO<T>(
    @field:Schema(description = "Total search result count, useful for fetching multiple pages")
    val totalCount: Long,
    @field:Schema(description = "The page number") val page: Int,
    @field:Schema(description = "The page size") val pageSize: Int,
    @field:Schema(description = "List of search results") val results: List<T>,
) {}
