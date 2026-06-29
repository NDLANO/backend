/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.NodeType

@Schema
data class NodeSearchBody(
    @field:Schema(
        description =
            "If specified, the search result will be filtered by whether they include the key,value combination provided. If more than one provided only one will be required (OR)")
    val customFields: Map<String, String>? = null,
    @field:Schema(description = "ISO-639-1 language code", example = "nb")
    @param:JsonSetter(nulls = Nulls.SKIP)
    val language: String = Constants.DefaultLanguage,
    @param:JsonSetter(nulls = Nulls.SKIP)
    @field:Schema(description = "How many results to return per page")
    val pageSize: Int = 10,
    @param:JsonSetter(nulls = Nulls.SKIP)
    @field:Schema(description = "Which page to fetch")
    val page: Int = 1,
    @field:Schema(description = "Query to search names") val query: String? = null,
    @field:Schema(description = "Ids to fetch for query") val ids: List<String>? = null,
    @field:Schema(description = "ContentURIs to fetch for query")
    val contentUris: List<String>? = null,
    @field:Schema(description = "Filter by nodeType") val nodeType: List<NodeType>? = null,
    @param:JsonSetter(nulls = Nulls.SKIP)
    @field:Schema(description = "Include all contexts")
    val includeContexts: Boolean = true,
    @param:JsonSetter(nulls = Nulls.SKIP)
    @field:Schema(description = "Filter out programme contexts")
    val filterProgrammes: Boolean = true,
    @field:Schema(description = "Id to root id in context.") val rootId: URI? = null,
    @field:Schema(description = "Id to parent id in context.") val parentId: URI? = null,
)
