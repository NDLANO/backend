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
import jakarta.persistence.EntityManager
import java.net.URI
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.repositories.ResourceTypeRepository
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT
import no.ndla.taxonomy.service.dtos.TranslationDTO
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/v1/resource-types/{id}/translations", "/v1/resource-types/{id}/translations/"])
class ResourceTypeTranslations(
    private val resourceTypeRepository: ResourceTypeRepository,
    private val entityManager: EntityManager,
) {

  @GetMapping
  @Operation(summary = "Gets all relevanceTranslations for a single resource type")
  @Transactional(readOnly = true)
  fun getAllResourceTypeTranslations(@PathVariable("id") id: URI): List<TranslationDTO> {
    val resourceType = resourceTypeRepository.getByPublicId(id)
    return resourceType.translations.map { rt -> TranslationDTO(rt.languageCode, rt.name) }
  }

  @GetMapping("/{language}")
  @Operation(summary = "Gets a single translation for a single resource type")
  @Transactional(readOnly = true)
  fun getResourceTypeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
  ): TranslationDTO {
    val resourceType = resourceTypeRepository.getByPublicId(id)
    val translation =
        resourceType.getTranslation(language).orElseThrow {
          NotFoundException("translation with language code $language for resource type", id)
        }
    return TranslationDTO(translation.languageCode, translation.name)
  }

  @PutMapping("/{language}")
  @Operation(
      summary = "Creates or updates a translation of a resource type",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
  @Transactional
  fun createUpdateResourceTypeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
      @Parameter(name = "resourceType", description = "The new or updated translation")
      @RequestBody
      command: TranslationPUT,
  ) {
    val resourceType = resourceTypeRepository.getByPublicId(id)
    resourceType.addTranslation(command.name, language)
    entityManager.persist(resourceType)
  }

  @DeleteMapping("/{language}")
  @Operation(
      summary = "Deletes a translation",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
  @Transactional
  fun deleteResourceTypeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
  ) {
    val resourceType = resourceTypeRepository.getByPublicId(id)
    resourceType.getTranslation(language).ifPresent { _ ->
      resourceType.removeTranslation(language)
      entityManager.persist(resourceType)
    }
  }
}
