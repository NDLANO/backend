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
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.rest.v1.commands.SubjectPostPut
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
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
@RequestMapping(path = ["/v1/subjects", "/v1/subjects/"])
@Deprecated("Use /v1/nodes")
class Subjects(private val nodes: Nodes) {

    @GetMapping
    @Operation(summary = "Gets all subjects")
    @Transactional(readOnly = true)
    fun getAllSubjects(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
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
        Optional.of(listOf(NodeType.SUBJECT)),
        language,
        Optional.empty(),
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
    @Operation(summary = "Search all subjects")
    @Transactional(readOnly = true)
    fun searchSubjects(
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
        Optional.of(listOf(NodeType.SUBJECT)),
        true,
        true,
        Optional.empty(),
        Optional.empty(),
    )

    @GetMapping("/page")
    @Operation(summary = "Gets all nodes paginated")
    @Transactional(readOnly = true)
    fun getSubjectPage(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "The page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Size of page to fetch")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
    ): SearchResultDTO<NodeDTO> = nodes.getNodePage(language, page, pageSize, Optional.of(NodeType.SUBJECT), true, true, true)

    @GetMapping("/{id}")
    @Operation(
        summary = "Gets a single subject",
        description = "Default language will be returned if desired language not found or if parameter is omitted.",
    )
    @Transactional(readOnly = true)
    fun getSubject(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): NodeDTO = nodes.getNode(id, Optional.empty(), Optional.empty(), true, true, true, language)

    @PutMapping("/{id}")
    @Operation(
        summary = "Updates a subject",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun updateSubject(
        @PathVariable("id") id: URI,
        @Parameter(name = "subject", description = "The updated subject. Fields not included will be set to null.")
        @RequestBody
        @Schema(name = "SubjectPOST")
        command: SubjectPostPut,
    ) {
        nodes.updateEntity(id, command)
    }

    @PostMapping
    @Operation(
        summary = "Creates a new subject",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun createSubject(
        @Parameter(name = "subject", description = "The new subject")
        @RequestBody
        @Schema(name = "SubjectPUT")
        command: SubjectPostPut,
    ): ResponseEntity<Unit> = nodes.createEntity(Node(NodeType.SUBJECT), command)

    @GetMapping("/{id}/topics")
    @Operation(
        summary = "Gets all children associated with a subject",
        description = "This resource is read-only. To update the relationship between nodes, use the resource /subject-topics.",
    )
    @Transactional(readOnly = true)
    fun getSubjectChildren(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "If true, subtopics are fetched recursively")
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        recursive: Boolean,
        @Parameter(description = "Select by relevance. If not specified, all nodes will be returned.")
        @RequestParam(value = "relevance", required = false)
        relevance: Optional<URI>,
    ): List<NodeChildDTO> {
        val children = nodes.getChildren(
            id,
            Optional.of(listOf(NodeType.TOPIC)),
            listOf(NodeConnectionType.BRANCH),
            recursive,
            language,
            true,
            true,
            true,
        )
        return relevance
            .map { rel -> children.filter { node -> node.relevanceId.isPresent && node.relevanceId.get() == rel } }
            .orElse(children)
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deletes a single entity by id",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        nodes.deleteEntity(id)
    }

    @GetMapping("/{subjectId}/resources")
    @Operation(
        summary = "Gets all resources for a subject. Searches recursively in all children of this node. The ordering of resources will be based on the rank of resources relative to the node they belong to.",
        tags = ["subjects"],
    )
    @Transactional(readOnly = true)
    fun getSubjectResources(
        @PathVariable("subjectId") subjectId: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(
            description = "Filter by resource type id(s). If not specified, resources of all types will be returned. Multiple ids may be separated with comma or the parameter may be repeated for each id.",
        )
        @RequestParam(value = "type", required = false)
        resourceTypeIds: Optional<List<URI>>,
        @Parameter(description = "Select by relevance. If not specified, all resources will be returned.")
        @RequestParam(value = "relevance", required = false)
        relevance: Optional<URI>,
    ): List<NodeChildDTO> = nodes.getResources(subjectId, language, true, true, true, true, resourceTypeIds, relevance)

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
