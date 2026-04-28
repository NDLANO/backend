/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.ndla.taxonomy.domain.DomainEntity
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException
import no.ndla.taxonomy.repositories.NodeConnectionRepository
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT
import no.ndla.taxonomy.rest.v1.dtos.NodeResourceDTO
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePOST
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.NodeConnectionService
import no.ndla.taxonomy.service.dtos.MetadataDTO
import no.ndla.taxonomy.service.dtos.SearchResultDTO
import org.springframework.data.domain.PageRequest
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

@RestController
@RequestMapping(path = ["/v1/node-resources", "/v1/node-resources/"])
class NodeResources(
    private val nodeRepository: NodeRepository,
    private val connectionService: NodeConnectionService,
    private val nodeConnectionRepository: NodeConnectionRepository,
) {

    @GetMapping
    @Operation(summary = "Gets all connections between node and resources")
    @Transactional(readOnly = true)
    fun getAllNodeResources(): List<NodeResourceDTO> = nodeConnectionRepository
        .findAllByChildNodeType(NodeType.RESOURCE)
        .map { NodeResourceDTO(it) }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and resources paginated")
    @Transactional(readOnly = true)
    fun getNodeResourcesPage(
        @Parameter(description = "The page to fetch", required = true)
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Size of page to fetch", required = true)
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
    ): SearchResultDTO<NodeResourceDTO> {
        require(page >= 1) { "page parameter must be bigger than 0" }

        val pageRequest = PageRequest.of(page - 1, pageSize)
        val connections = nodeConnectionRepository.findIdsPaginatedByChildNodeType(pageRequest, NodeType.RESOURCE)
        val ids = connections.content.map(DomainEntity::getId)
        val results = nodeConnectionRepository.findByIds(ids)
        val contents = results.map { NodeResourceDTO(it) }
        return SearchResultDTO(connections.totalElements, page, pageSize, contents)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a node and a resource")
    @Transactional(readOnly = true)
    fun getNodeResource(@PathVariable("id") id: URI): NodeResourceDTO {
        val connection = nodeConnectionRepository.getByPublicId(id)
        return NodeResourceDTO(connection)
    }

    @PostMapping
    @Operation(
        summary = "Adds a resource to a node",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Created201ApiResponse
    @Transactional
    fun createNodeResource(
        @Parameter(name = "connection", description = "new node/resource connection ")
        @RequestBody
        command: NodeResourcePOST,
    ): ResponseEntity<Unit> {
        val parent = nodeRepository.getByPublicId(command.nodeId)
        val child = nodeRepository.getByPublicId(command.resourceId)
        val relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")))
        val rank = command.rank.orElse(null)

        val nodeConnection = connectionService.connectParentChild(
            parent,
            child,
            relevance,
            rank,
            command.primary,
            NodeConnectionType.BRANCH,
        )

        val location = URI.create("/node-resources/${nodeConnection.publicId}")
        return ResponseEntity.created(location).build()
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Removes a resource from a node",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        val connection = nodeConnectionRepository.getByPublicId(id)
        connectionService.disconnectParentChildConnection(connection)
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Updates a connection between a node and a resource",
        description = "Use to update which node is primary to the resource or to change sorting order.",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun updateNodeResource(
        @PathVariable("id") id: URI,
        @Parameter(name = "connection", description = "Updated node/resource connection")
        @RequestBody
        command: NodeResourcePUT,
    ) {
        val nodeResource = nodeConnectionRepository.getByPublicId(id)
        val relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")))
        if (nodeResource.isPrimary().orElse(false) && !command.primary.orElse(false)) {
            throw PrimaryParentRequiredException()
        }

        connectionService.updateParentChild(nodeResource, relevance, command.rank, command.primary)
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Gets metadata for entity")
    @Transactional(readOnly = true)
    fun getMetadata(@PathVariable("id") id: URI): MetadataDTO {
        val connection = nodeConnectionRepository.findByPublicId(id) ?: throw NotFoundException("Connection", id)
        return MetadataDTO(connection.metadata)
    }

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
    ): MetadataDTO {
        val connection = nodeConnectionRepository.findByPublicId(id) ?: throw NotFoundException("Connection", id)
        val result = connection.metadata.mergeWith(entityToUpdate)
        return MetadataDTO(result)
    }
}
