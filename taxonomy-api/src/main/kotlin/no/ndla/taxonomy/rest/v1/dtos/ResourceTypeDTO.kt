/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.service.dtos.TranslationDTO

@Schema(
    name = "ResourceType",
    requiredProperties = ["id", "name", "translations", "supportedLanguages"],
)
data class ResourceTypeDTO(
    @field:Schema(example = "urn:resourcetype:1") val id: URI,
    @field:Schema(description = "The name of the resource type", example = "Lecture")
    val name: String,
    @field:Schema(
        description = "Sub resource types",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val subtypes: List<ResourceTypeDTO>? = null,
    @field:Schema(description = "All translations of this resource type")
    val translations: Set<TranslationDTO>,
    @field:Schema(description = "List of language codes supported by translations")
    val supportedLanguages: Set<String>,
    @field:Schema(description = "Sort order of the resource type", example = "1") val order: Int,
) {
  constructor(
      resourceType: ResourceType,
      language: String,
      subtypes: List<ResourceTypeDTO> = emptyList(),
  ) : this(
      id = resourceType.publicId,
      translations = resourceType.translations.map(::TranslationDTO).toSet(),
      supportedLanguages = resourceType.supportedLanguages,
      name = resourceType.getTranslatedName(language),
      order = resourceType.order,
      subtypes = subtypes,
  )
}
