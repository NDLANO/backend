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
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository
import no.ndla.taxonomy.rest.v1.commands.ResourcePostPut
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.NodeService
import no.ndla.taxonomy.service.dtos.MetadataDTO
import no.ndla.taxonomy.service.dtos.NodeDTO
import no.ndla.taxonomy.service.dtos.NodeWithParents
import no.ndla.taxonomy.service.dtos.ResourceTypeWithConnectionDTO
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
@RequestMapping(path = ["/v1/resources", "/v1/resources/"])
@Deprecated("Use /v1/nodes")
@Suppress("DEPRECATION")
class Resources(
    private val nodes: Nodes,
    private val resourceResourceTypeRepository: ResourceResourceTypeRepository,
    private val nodeService: NodeService,
) {

    private val location = "/v1/resources"

    @GetMapping
    @Operation(summary = "Lists all resources")
    @Transactional(readOnly = true)
    fun getAllResources(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
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
        Optional.of(listOf(NodeType.RESOURCE)),
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
    @Operation(summary = "Search all resources")
    @Transactional(readOnly = true)
    fun searchResources(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
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
        Optional.of(listOf(NodeType.RESOURCE)),
        true,
        true,
        Optional.empty(),
        Optional.empty(),
    )

    @GetMapping("/page")
    @Operation(summary = "Gets all resources paginated")
    @Transactional(readOnly = true)
    fun getResourcePage(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "The page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Size of page to fetch")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
    ): SearchResultDTO<NodeDTO> = nodes.getNodePage(language, page, pageSize, Optional.of(NodeType.RESOURCE), true, true, true)

    @GetMapping("{id}")
    @Operation(summary = "Gets a single resource")
    @Transactional(readOnly = true)
    fun getResource(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): NodeDTO = nodes.getNode(id, Optional.empty(), Optional.empty(), true, true, true, language)

    @PutMapping("{id}")
    @Operation(
        summary = "Updates a resource",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun updateResource(
        @PathVariable("id") id: URI,
        @Parameter(name = "resource", description = "the updated resource. Fields not included will be set to null.")
        @RequestBody
        @Schema(name = "ResourcePOST")
        command: ResourcePostPut,
    ) {
        nodes.updateEntity(id, command)
    }

    @PostMapping
    @Operation(
        summary = "Adds a new resource",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun createResource(
        @Parameter(name = "resource", description = "the new resource")
        @RequestBody
        @Schema(name = "ResourcePUT")
        command: ResourcePostPut,
    ): ResponseEntity<Unit> = nodes.createEntity(Node(NodeType.RESOURCE), command)

    @PostMapping("{id}/clone")
    @Operation(
        summary = "Clones a resource, including resource-types and translations",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun cloneResource(
        @Parameter(description = "Id of resource to clone", example = "urn:resource:1")
        @PathVariable("id")
        publicId: URI,
        @Parameter(name = "resource", description = "Object containing contentUri. Other values are ignored.")
        @RequestBody
        @Schema(name = "ResourcePOST")
        command: ResourcePostPut,
    ): ResponseEntity<Unit> {
        val entity = nodeService.cloneNode(publicId, Optional.ofNullable(command.contentUri))
        val locationUri = URI.create("$location/${entity.publicId}")
        return ResponseEntity.created(locationUri).build()
    }

    @GetMapping("{id}/resource-types")
    @Operation(summary = "Gets all resource types associated with this resource")
    @Transactional(readOnly = true)
    fun getResourceTypes(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): List<ResourceTypeWithConnectionDTO> = resourceResourceTypeRepository
        .resourceResourceTypeByParentId(id)
        .map { ResourceTypeWithConnectionDTO(it, language) }

    @GetMapping("{id}/full")
    @Operation(summary = "Gets all parent topics, all filters and resourceTypes for this resource")
    @Transactional(readOnly = true)
    fun getResourceFull(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): NodeWithParents = nodes.getNodeFull(id, language, true)

    @DeleteMapping("{id}")
    @Operation(
        summary = "Deletes a single entity by id",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        nodes.deleteEntity(id)
    }

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
