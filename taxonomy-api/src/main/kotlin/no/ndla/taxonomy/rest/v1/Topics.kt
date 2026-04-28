/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.rest.v1.commands.TopicPostPut
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.dtos.ConnectionDTO
import no.ndla.taxonomy.service.dtos.MetadataDTO
import no.ndla.taxonomy.service.dtos.NodeChildDTO
import no.ndla.taxonomy.service.dtos.NodeDTO
import no.ndla.taxonomy.service.dtos.SearchResultDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.Optional

@RestController
@RequestMapping(path = ["/v1/topics", "/v1/topics/"])
@Deprecated("Use /v1/nodes")
class Topics(private val nodes: Nodes) {

    @GetMapping
    @Operation(summary = "Gets all topics")
    @Transactional(readOnly = true)
    fun getAllTopics(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "Filter by contentUri")
        @RequestParam(value = "contentURI", required = false)
        contentUri: Optional<URI>,
        @Parameter(description = "Filter by key and value")
        @RequestParam(value = "key", required = false)
        key: Optional<String>,
        @Parameter(description = "Filter by key and value")
        @RequestParam(value = "value", required = false)
        value: Optional<String>,
        @Parameter(description = "Filter contexts by visibility")
        @RequestParam(value = "isVisible", required = false)
        isVisible: Optional<Boolean>,
    ): List<NodeDTO> = nodes.getAllNodes(
        Optional.of(listOf(NodeType.TOPIC)),
        language,
        contentUri,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        key,
        value,
        Optional.empty(),
        isVisible,
        true,
        true,
        Optional.empty(),
        Optional.empty(),
    )

    @GetMapping("/search")
    @Operation(summary = "Search all topics")
    @Transactional(readOnly = true)
    fun searchTopics(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "How many results to return per page")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
        @Parameter(description = "Which page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Query to search names")
        @RequestParam(value = "query", required = false)
        query: Optional<String>,
        @Parameter(description = "Ids to fetch for query")
        @RequestParam(value = "ids", required = false)
        ids: Optional<List<String>>,
        @Parameter(description = "ContentURIs to fetch for query")
        @RequestParam(value = "contentUris", required = false)
        contentUris: Optional<List<String>>,
    ): SearchResultDTO<NodeDTO> = nodes.searchNodes(
        language,
        pageSize,
        page,
        query,
        ids,
        contentUris,
        Optional.of(listOf(NodeType.TOPIC)),
        true,
        true,
        Optional.empty(),
        Optional.empty(),
    )

    @GetMapping("/page")
    @Operation(summary = "Gets all topics paginated")
    @Transactional(readOnly = true)
    fun getTopicsPage(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "The page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Size of page to fetch")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
    ): SearchResultDTO<NodeDTO> = nodes.getNodePage(language, page, pageSize, Optional.of(NodeType.TOPIC), true, true, true)

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single topic")
    @Transactional(readOnly = true)
    fun getTopic(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): NodeDTO = nodes.getNode(id, Optional.empty(), Optional.empty(), true, true, true, language)

    @PostMapping
    @Operation(
        summary = "Creates a new topic",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun createTopic(
        @Parameter(name = "connection", description = "The new topic")
        @RequestBody
        @Schema(name = "TopicPOST")
        command: TopicPostPut,
    ): ResponseEntity<Unit> = nodes.createEntity(no.ndla.taxonomy.domain.Node(NodeType.TOPIC), command)

    @PutMapping("/{id}")
    @Operation(
        summary = "Updates a single topic",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun updateTopic(
        @PathVariable("id") id: URI,
        @Parameter(name = "topic", description = "The updated topic. Fields not included will be set to null.")
        @RequestBody
        command: TopicPostPut,
    ) {
        nodes.updateEntity(id, command)
    }

    @GetMapping("/{id}/topics")
    @Operation(summary = "Gets all subtopics for this topic")
    @Transactional(readOnly = true)
    fun getTopicSubTopics(
        @Parameter(name = "id", required = true) @PathVariable("id") id: URI,
        @Parameter(
            description = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.",
        )
        @RequestParam(value = "subject", required = false, defaultValue = "")
        subjectId: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): List<NodeChildDTO> = nodes.getChildren(
        id,
        Optional.of(listOf(NodeType.TOPIC)),
        listOf(NodeConnectionType.BRANCH),
        false,
        language,
        true,
        true,
        true,
    )

    @GetMapping("/{id}/connections")
    @Operation(summary = "Gets all subjects and subtopics this topic is connected to")
    @Transactional(readOnly = true)
    fun getAllTopicConnections(@PathVariable("id") id: URI): List<ConnectionDTO> = nodes.getAllConnections(id)

    @DeleteMapping("/{id}")
    @Operation(
        description = "Deletes a single entity by id",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        nodes.deleteEntity(id)
    }

    @GetMapping("/{id}/resources")
    @Operation(
        summary = "Gets all resources for the given topic",
        tags = ["topics"],
    )
    @Transactional(readOnly = true)
    fun getTopicResources(
        @Parameter(name = "id", required = true) @PathVariable("id") topicId: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "If true, resources from subtopics are fetched recursively")
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        recursive: Boolean,
        @Parameter(
            description = "Select by resource type id(s). If not specified, resources of all types will be returned. Multiple ids may be separated with comma or the parameter may be repeated for each id.",
        )
        @RequestParam(value = "type", required = false)
        resourceTypeIds: Optional<List<URI>>,
        @Parameter(description = "Select by relevance. If not specified, all resources will be returned.")
        @RequestParam(value = "relevance", required = false)
        relevance: Optional<URI>,
    ): List<NodeChildDTO> = nodes.getResources(topicId, language, true, true, true, recursive, resourceTypeIds, relevance)

    @PutMapping("/{id}/makeResourcesPrimary")
    @Operation(
        summary = "Makes all connected resources primary",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    fun makeResourcesPrimary(
        @Parameter(name = "id", required = true) @PathVariable("id") nodeId: URI,
        @Parameter(description = "If true, children are fetched recursively")
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        recursive: Boolean,
    ): ResponseEntity<Boolean> = nodes.makeResourcesPrimary(nodeId, recursive)

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Gets metadata for entity")
    @Transactional(readOnly = true)
    fun getMetadata(@PathVariable("id") id: URI): MetadataDTO = nodes.getMetadata(id)

    @PutMapping("/{id}/metadata")
    @Operation(
        summary = "Updates metadata for entity",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun putMetadata(
        @PathVariable("id") id: URI,
        @RequestBody entityToUpdate: MetadataPUT,
    ): MetadataDTO = nodes.putMetadata(id, entityToUpdate)
}
