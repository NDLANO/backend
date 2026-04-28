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
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException
import no.ndla.taxonomy.repositories.NodeConnectionRepository
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionDTO
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionPOST
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionPUT
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
import java.util.concurrent.atomic.AtomicInteger

@RestController
@RequestMapping(path = ["/v1/node-connections", "/v1/node-connections/"])
class NodeConnections(
    private val nodeRepository: NodeRepository,
    private val nodeConnectionRepository: NodeConnectionRepository,
    private val connectionService: NodeConnectionService,
) {

    @GetMapping
    @Operation(summary = "Gets all connections between node and children")
    @Transactional(readOnly = true)
    fun getAllNodeConnections(): List<NodeConnectionDTO> {
        val ids = nodeConnectionRepository.findAllIds()
        val counter = AtomicInteger()
        val toReturn = mutableListOf<NodeConnectionDTO>()
        ids.groupBy { counter.getAndIncrement() / 1000 }
            .values
            .forEach { idChunk ->
                val connections = nodeConnectionRepository.findByIds(idChunk)
                toReturn.addAll(connections.map { NodeConnectionDTO(it) })
            }
        return toReturn
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and children paginated")
    @Transactional(readOnly = true)
    fun getNodeConnectionsPage(
        @Parameter(description = "The page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Size of page to fetch")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
    ): SearchResultDTO<NodeConnectionDTO> {
        require(page >= 1) { "page parameter must be bigger than 0" }
        val ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page - 1, pageSize))
        val results = nodeConnectionRepository.findByIds(ids.content)
        val contents = results.map { NodeConnectionDTO(it) }
        return SearchResultDTO(ids.totalElements, page, pageSize, contents)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single connection between a node and a child")
    @Transactional(readOnly = true)
    fun getNodeConnection(@PathVariable("id") id: URI): NodeConnectionDTO {
        val topicSubtopic = nodeConnectionRepository.getByPublicId(id)
        return NodeConnectionDTO(topicSubtopic)
    }

    @PostMapping
    @Operation(
        summary = "Adds a node to a parent",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun createNodeConnection(
        @Parameter(name = "connection", description = "The new connection")
        @RequestBody
        command: NodeConnectionPOST,
    ): ResponseEntity<Unit> {
        val parent = nodeRepository.getByPublicId(command.parentId)
        val child = nodeRepository.getByPublicId(command.childId)
        val relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")))
        val rank = command.rank.orElse(null)
        val connectionType = command.connectionType.orElse(NodeConnectionType.BRANCH)
        val nodeConnection = connectionService.connectParentChild(parent, child, relevance, rank, command.primary, connectionType)

        val location = URI.create("/node-child/${nodeConnection.publicId}")
        return ResponseEntity.created(location).build()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Removes a connection between a node and a child",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        val connection = nodeConnectionRepository.getByPublicId(id)
        connectionService.disconnectParentChildConnection(connection)
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Updates a connection between a node and a child",
        description = "Use to update which node is primary to a child or to alter sorting order",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun updateNodeConnection(
        @PathVariable("id") id: URI,
        @Parameter(name = "connection", description = "The updated connection")
        @RequestBody
        command: NodeConnectionPUT,
    ) {
        val connection = nodeConnectionRepository.getByPublicId(id)
        val relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")))
        if (connection.isPrimary().orElse(false) && !command.primary.orElse(false)) {
            throw PrimaryParentRequiredException()
        }

        connectionService.updateParentChild(connection, relevance, command.rank, command.primary)
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
