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
import no.ndla.taxonomy.service.dtos.TranslationDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Deprecated("Use /v1/resource-types")
@RequestMapping(
    path = ["/v1/resource-types/{id}/translations", "/v1/resource-types/{id}/translations/"])
class ResourceTypeTranslations {

  private fun getResourceType(id: URI) =
      ResourceType.findByPublicId(id) ?: throw NotFoundException("ResourceType", id)

  @GetMapping
  @Operation(summary = "Gets all relevanceTranslations for a single resource type")
  fun getAllResourceTypeTranslations(@PathVariable("id") id: URI): List<TranslationDTO> =
      getResourceType(id).translations.mapNotNull { t ->
        val lang = t.languageCode ?: return@mapNotNull null
        val name = t.name ?: return@mapNotNull null
        TranslationDTO(lang, name)
      }

  @GetMapping("/{language}")
  @Operation(summary = "Gets a single translation for a single resource type")
  fun getResourceTypeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
  ): TranslationDTO {
    val resourceType = getResourceType(id)
    val translation =
        resourceType.translations.firstOrNull { it.languageCode == language }
            ?: throw NotFoundException(
                "translation with language code $language for resource type",
                id,
            )
    return TranslationDTO(translation.languageCode!!, translation.name!!)
  }
}
