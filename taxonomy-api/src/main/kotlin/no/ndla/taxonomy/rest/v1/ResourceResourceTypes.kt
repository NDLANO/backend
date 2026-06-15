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
import java.net.URI
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypeDTO
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypePOST
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.NodeService
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Deprecated("Use /v1/nodes")
@RequestMapping(path = ["/v1/resource-resourcetypes", "/v1/resource-resourcetypes/"])
class ResourceResourceTypes(
    private val nodeService: NodeService,
    private val nodeRepository: NodeRepository,
) {

  @PostMapping
  @Operation(
      summary = "Adds a resource type to a resource",
      security = [SecurityRequirement(name = "oauth")],
  )
  @Created201ApiResponse
  @Deprecated("Use /v1/node/{id}")
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun createResourceResourceType(
      @Parameter(name = "connection", description = "The new resource/resource type connection")
      @RequestBody
      command: ResourceResourceTypePOST
  ): ResponseEntity<Unit> {
    val node = nodeService.getNode(command.resourceId)
    if (node.nodeType !== NodeType.RESOURCE) {
      throw InvalidArgumentServiceException(
          "Node with id ${command.resourceId} is not of type RESOURCE")
    }
    val rt =
        ResourceType.findByPublicId(command.resourceTypeId)
            ?: throw InvalidArgumentServiceException(
                "Unknown resource type: ${command.resourceTypeId}")
    node.addResourceType(rt)
    nodeRepository.save(node)
    val rrt =
        ResourceResourceTypeDTO(
            resourceId = node.publicId,
            resourceTypeId = rt.publicId,
        )
    val location = URI.create("/resource-resourcetypes/${rrt.id}")
    return ResponseEntity.created(location).build()
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Removes a resource type from a resource",
      security = [SecurityRequirement(name = "oauth")],
  )
  @Deprecated("Use /v1/node/{id}")
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun deleteResourceResourceType(@PathVariable("id") id: URI) {
    val (nodeId, resourceTypeId) = ResourceResourceTypeDTO.parseConnectionId(id)
    val node = nodeService.getNode(nodeId)
    val rt =
        ResourceType.findByPublicId(resourceTypeId)
            ?: throw InvalidArgumentServiceException("Unknown resource type: $resourceTypeId")
    node.removeResourceType(rt)
    nodeRepository.save(node)
  }

  @GetMapping
  @Operation(summary = "Gets all connections between resources and resource types")
  @Transactional(readOnly = true)
  fun getAllResourceResourceTypes(): List<ResourceResourceTypeDTO> =
      nodeRepository.findAllResourceTypeConnections().map { row ->
        ResourceResourceTypeDTO(
            resourceId = URI.create(row[0] as String),
            resourceTypeId = URI.create(row[1] as String),
        )
      }

  @GetMapping("/{id}")
  @Operation(summary = "Gets a single connection between resource and resource type")
  @Transactional(readOnly = true)
  fun getResourceResourceType(@PathVariable("id") id: URI): ResourceResourceTypeDTO {
    val (nodeId, resourceTypeId) = ResourceResourceTypeDTO.parseConnectionId(id)
    val node = nodeService.getNode(nodeId)

    node.resourceTypes
        .find { it.publicId == resourceTypeId }
        ?.let {
          return ResourceResourceTypeDTO(node.publicId, it.publicId)
        }

    throw NotFoundException("Failed to find resource resource type with id $id")
  }
}
