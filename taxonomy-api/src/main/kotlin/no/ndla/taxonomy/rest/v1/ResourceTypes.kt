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
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.repositories.ResourceTypeRepository
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypePUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.ResourceTypeService
import no.ndla.taxonomy.service.UpdatableDto
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
import java.net.URI

@RestController
@RequestMapping(path = ["/v1/resource-types", "/v1/resource-types/"])
class ResourceTypes(
    private val resourceTypeRepository: ResourceTypeRepository,
    private val resourceTypeService: ResourceTypeService,
) : BaseCrudController<ResourceType> {

    private val location: String by lazy { controllerLocation(javaClass) }

    @GetMapping
    @Operation(summary = "Gets a list of all resource types")
    @Transactional(readOnly = true)
    fun getAllResourceTypes(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = "")
        language: String,
    ): List<ResourceTypeDTO> = resourceTypeRepository
        .findAllByParentIncludingTranslationsAndFirstLevelSubtypes(null)
        .map { ResourceTypeDTO(it, language, 100) }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single resource type")
    @Transactional(readOnly = true)
    fun getResourceType(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = "")
        language: String,
    ): ResourceTypeDTO = resourceTypeRepository
        .findFirstByPublicIdIncludingTranslations(id)
        .map { ResourceTypeDTO(it, language, 0) }
        .orElseThrow { NotFoundException("ResourceType", id) }

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
            val parent = resourceTypeRepository.getByPublicId(command.parentId)
            resourceType.setParent(parent)
        }
        if (command.order > -1) {
            resourceType.order = command.order
        }
        return createEntity(resourceType, command)
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Updates a resource type. Use to update which resource type is parent. You can also update the id, take care!",
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
        val resourceType = updateEntity(id, command)

        val parent = command.parentId?.let { resourceTypeRepository.getByPublicId(it) }
        resourceType.setParent(parent)
        if (command.id != null) {
            resourceType.publicId = command.id
        }
        if (command.order > -1) {
            resourceType.order = command.order
        }
    }

    @GetMapping("/{id}/subtypes")
    @Operation(summary = "Gets subtypes of one resource type")
    @Transactional(readOnly = true)
    fun getResourceTypeSubtypes(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = "")
        language: String,
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        @Parameter(description = "If true, sub resource types are fetched recursively")
        recursive: Boolean,
    ): List<ResourceTypeDTO> = resourceTypeRepository
        .findAllByParentPublicIdIncludingTranslationsAndFirstLevelSubtypes(id)
        .map { ResourceTypeDTO(it, language, 100) }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deletes a single entity by id",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    override fun deleteEntity(@PathVariable("id") id: URI) {
        val entity = resourceTypeRepository.getByPublicId(id)
        resourceTypeRepository.delete(entity)
        resourceTypeRepository.flush()
        resourceTypeService.updateOrderAfterDelete()
    }

    @Transactional
    override fun createEntity(entity: ResourceType, command: UpdatableDto<ResourceType>): ResponseEntity<Unit> {
        return try {
            validateAndAssignId(entity, command)
            command.apply(entity)
            resourceTypeRepository.saveAndFlush(entity)
            resourceTypeService.shiftOrderAfterInsertUpdate(entity)
            ResponseEntity.created(URI.create("$location/${entity.publicId}")).build()
        } catch (e: DataIntegrityViolationException) {
            handleDuplicateId(command)
        }
    }

    @Transactional
    override fun updateEntity(id: URI, command: UpdatableDto<ResourceType>): ResourceType {
        val entity = resourceTypeRepository.getByPublicId(id)
        validateUrn(id, entity)
        command.apply(entity)
        resourceTypeService.shiftOrderAfterInsertUpdate(entity)
        return entity
    }
}
