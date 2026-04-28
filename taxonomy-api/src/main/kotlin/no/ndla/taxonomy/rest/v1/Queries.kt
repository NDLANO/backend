/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO
import no.ndla.taxonomy.service.NodeService
import no.ndla.taxonomy.service.dtos.NodeDTO
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.Optional

@RestController
@RequestMapping(path = ["/v1/queries"])
@Suppress("DEPRECATION")
class Queries(
    private val topicController: Topics,
    private val resourceController: Resources,
    private val nodeService: NodeService,
) {

    @GetMapping("/{contentURI}")
    @Operation(summary = "Gets a list of contexts matching given contentURI, empty list if no matches are found.")
    @Transactional(readOnly = true)
    fun contextByContentURI(
        @PathVariable("contentURI") contentURI: Optional<URI>,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "Whether to filter out contexts if a parent (or the node itself) is non-visible")
        @RequestParam(value = "filterVisibles", required = false, defaultValue = "true")
        filterVisibles: Boolean,
    ): List<TaxonomyContextDTO> = nodeService.getSearchableByContentUri(contentURI, filterVisibles, language)

    @GetMapping("/contextId")
    @Operation(summary = "Gets a list of contexts matching given contextId, empty list if no matches are found.")
    @Transactional(readOnly = true)
    fun contextByContextId(
        @RequestParam("contextId") contextId: Optional<String>,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
    ): List<TaxonomyContextDTO> = nodeService.getContextByContextId(contextId, language)

    @GetMapping("/path")
    @Operation(
        summary = "Gets a list of contexts matching given pretty url with contextId, empty list if no matches are found.",
    )
    @Transactional(readOnly = true)
    fun queryPath(
        @RequestParam("path") path: Optional<String>,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
    ): List<TaxonomyContextDTO> = nodeService.getContextByPath(path, language)

    @GetMapping("/resources")
    @Operation(
        summary = "Gets a list of resources matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/resources?contentURI= instead",
    )
    @Transactional(readOnly = true)
    @Deprecated("Use /v1/resources?contentURI=", level = DeprecationLevel.WARNING)
    fun queryResources(
        @RequestParam("contentURI") contentURI: Optional<URI>,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "Filter by key and value")
        @RequestParam(value = "key", required = false)
        key: Optional<String>,
        @Parameter(description = "Fitler by key and value")
        @RequestParam(value = "value", required = false)
        value: Optional<String>,
        @Parameter(description = "Filter by visible")
        @RequestParam(value = "isVisible", required = false)
        isVisible: Optional<Boolean>,
    ): List<NodeDTO> = resourceController.getAllResources(language, contentURI, key, value, isVisible)

    @GetMapping("/topics")
    @Operation(
        summary = "Gets a list of topics matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/topics?contentURI= instead",
    )
    @Transactional(readOnly = true)
    @Deprecated("Use /v1/topics?contentURI=", level = DeprecationLevel.WARNING)
    fun queryTopics(
        @RequestParam("contentURI") contentURI: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "Filter by key and value")
        @RequestParam(value = "key", required = false)
        key: Optional<String>,
        @Parameter(description = "Fitler by key and value")
        @RequestParam(value = "value", required = false)
        value: Optional<String>,
        @Parameter(description = "Filter by visible")
        @RequestParam(value = "isVisible", required = false)
        isVisible: Optional<Boolean>,
    ): List<NodeDTO> = topicController.getAllTopics(language, Optional.of(contentURI), key, value, isVisible)
}
