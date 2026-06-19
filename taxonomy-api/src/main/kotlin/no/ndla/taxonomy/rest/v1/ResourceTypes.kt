/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import java.net.URI
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/resource-types", "/v1/resource-types/"])
class ResourceTypes {

  @GetMapping
  @Operation(summary = "Gets a list of all resource types")
  @Transactional(readOnly = true)
  fun getAllResourceTypes(
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", required = false, defaultValue = "")
      language: String,
  ): List<ResourceTypeDTO> {
    val byParentId = ResourceType.entries.groupBy { it.parent?.publicId }
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
      ResourceType.findByPublicId(id)?.let { ResourceTypeDTO(it, language) }
          ?: throw NotFoundException("ResourceType", id)

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
      val byParentId = ResourceType.entries.groupBy { it.parent?.publicId }
      buildTree(byParentId, id, language)
    } else {
      ResourceType.entries
          .filter { it.parent?.publicId == id }
          .map { ResourceTypeDTO(it, language) }
    }
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
