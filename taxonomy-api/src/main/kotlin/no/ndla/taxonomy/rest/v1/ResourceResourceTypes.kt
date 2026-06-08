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
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypeDTO
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypePOST
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.NodeService
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
@RequestMapping(path = ["/v1/resource-resourcetypes", "/v1/resource-resourcetypes/"])
class ResourceResourceTypes(
    private val resourceResourceTypeRepository: ResourceResourceTypeRepository,
    private val nodeService: NodeService,
) {

  @PostMapping
  @Operation(
      summary = "Adds a resource type to a resource",
      security = [SecurityRequirement(name = "oauth")],
  )
  @Created201ApiResponse
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun createResourceResourceType(
      @Parameter(name = "connection", description = "The new resource/resource type connection")
      @RequestBody
      command: ResourceResourceTypePOST
  ): ResponseEntity<Unit> {
    val rrt = nodeService.connectNodeResourceType(command.resourceId, command.resourceTypeId)
    val location = URI.create("/resource-resourcetypes/${rrt.publicId}")
    return ResponseEntity.created(location).build()
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Removes a resource type from a resource",
      security = [SecurityRequirement(name = "oauth")],
  )
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun deleteResourceResourceType(@PathVariable("id") id: URI) {
    resourceResourceTypeRepository.delete(resourceResourceTypeRepository.getByPublicId(id))
    resourceResourceTypeRepository.flush()
  }

  @GetMapping
  @Operation(summary = "Gets all connections between resources and resource types")
  @Transactional(readOnly = true)
  fun getAllResourceResourceTypes(): List<ResourceResourceTypeDTO> {
    return resourceResourceTypeRepository
        .findAllIncludingResourceAndResourceType()
        .map(::ResourceResourceTypeDTO)
  }

  @GetMapping("/{id}")
  @Operation(summary = "Gets a single connection between resource and resource type")
  @Transactional(readOnly = true)
  fun getResourceResourceType(@PathVariable("id") id: URI): ResourceResourceTypeDTO {
    val res = resourceResourceTypeRepository.getByPublicId(id)
    return ResourceResourceTypeDTO(res)
  }
}
