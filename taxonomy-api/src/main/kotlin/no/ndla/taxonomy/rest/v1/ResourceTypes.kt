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
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.repositories.ResourceTypeRepository
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypePUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.ResourceTypeService
import org.springframework.dao.DataIntegrityViolationException
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

@RestController
@RequestMapping(path = ["/v1/resource-types", "/v1/resource-types/"])
class ResourceTypes(
    private val resourceTypeRepository: ResourceTypeRepository,
    private val resourceTypeService: ResourceTypeService,
) {

  private val location: String by lazy { controllerLocation(javaClass) }

  @GetMapping
  @Operation(summary = "Gets a list of all resource types")
  @Transactional(readOnly = true)
  fun getAllResourceTypes(
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", required = false, defaultValue = "")
      language: String,
  ): List<ResourceTypeDTO> {
    val byParentId =
        resourceTypeRepository.findAllByOrderByOrderAsc().groupBy { it.parent?.publicId }
    return buildTree(byParentId, null, language)
  }

  @GetMapping("/{id}")
  @Operation(summary = "Gets a single resource type")
  @Transactional(readOnly = true)
  fun getResourceType(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", required = false, defaultValue = "")
      language: String,
  ): ResourceTypeDTO =
      resourceTypeRepository.findFirstByPublicIdIncludingTranslations(id)?.let {
        ResourceTypeDTO(it, language)
      } ?: throw NotFoundException("ResourceType", id)

  @PostMapping
  @Operation(
      summary = "Adds a new resource type",
      security = [SecurityRequirement(name = "oauth")],
  )
  @Created201ApiResponse
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun createResourceType(
      @Parameter(name = "resourceType", description = "The new resource type")
      @RequestBody
      command: ResourceTypePUT,
  ): ResponseEntity<Unit> {
    val resourceType = ResourceType()
    if (command.parentId != null) {
      resourceType.parent = resourceTypeRepository.getByPublicId(command.parentId)
    }
    return try {
      command.id?.let {
        validateUrn(it, resourceType)
        resourceType.publicId = it
      }
      command.apply(resourceType)
      resourceTypeRepository.saveAndFlush(resourceType)
      resourceTypeService.shiftOrderAfterInsertUpdate(resourceType)
      ResponseEntity.created(URI.create("$location/${resourceType.publicId}")).build()
    } catch (e: DataIntegrityViolationException) {
      throw DuplicateIdException(command.id?.toString())
    }
  }

  @PutMapping("/{id}")
  @Operation(
      summary =
          "Updates a resource type. Use to update which resource type is parent. You can also update the id, take care!",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun updateResourceType(
      @PathVariable id: URI,
      @Parameter(
          name = "resourceType",
          description = "The updated resource type. Fields not included will be set to null.",
      )
      @RequestBody
      command: ResourceTypePUT,
  ) {
    val resourceType = resourceTypeRepository.getByPublicId(id)
    command.id?.let {
      validateUrn(it, resourceType)
      resourceType.publicId = it
    }
    command.apply(resourceType)
    resourceType.parent = command.parentId?.let { resourceTypeRepository.getByPublicId(it) }
    resourceTypeService.shiftOrderAfterInsertUpdate(resourceType)
  }

  @GetMapping("/{id}/subtypes")
  @Operation(summary = "Gets subtypes of one resource type")
  @Transactional(readOnly = true)
  fun getResourceTypeSubtypes(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", required = false, defaultValue = "")
      language: String,
      @RequestParam(value = "recursive", required = false, defaultValue = "true")
      @Parameter(description = "If true, sub resource types are fetched recursively")
      recursive: Boolean,
  ): List<ResourceTypeDTO> {
    return if (recursive) {
      val byParentId =
          resourceTypeRepository.findAllByOrderByOrderAsc().groupBy { it.parent?.publicId }
      buildTree(byParentId, id, language)
    } else {
      resourceTypeRepository.findByParentPublicId(id).map { ResourceTypeDTO(it, language) }
    }
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Deletes a single entity by id",
      security = [SecurityRequirement(name = "oauth")],
  )
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  fun deleteEntity(@PathVariable("id") id: URI) {
    val entity = resourceTypeRepository.getByPublicId(id)
    resourceTypeRepository.delete(entity)
    resourceTypeRepository.flush()
    resourceTypeService.updateOrderAfterDelete()
  }

  private fun buildTree(
      byParentId: Map<URI?, List<ResourceType>>,
      parentId: URI?,
      language: String,
  ): List<ResourceTypeDTO> =
      byParentId[parentId]?.map { rt ->
        ResourceTypeDTO(rt, language, buildTree(byParentId, rt.publicId, language))
      } ?: emptyList()
}
