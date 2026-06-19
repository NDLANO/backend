/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import java.util.TreeSet
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.service.dtos.TranslationDTO

@Schema(
    name = "ResourceTypeWithConnection",
    requiredProperties = ["id", "name", "connectionId", "translations", "supportedLanguages"],
)
data class ResourceTypeWithConnectionDTO(
    @field:Schema(example = "urn:resourcetype:2") val id: URI,
    @field:Schema(description = "Internal order of the resource types") val order: Int,
    @field:Schema(example = "urn:resourcetype:1") val parentId: URI? = null,
    @field:Schema(description = "The name of the resource type", example = "Lecture")
    val name: String,
    @field:Schema(description = "All translations of this resource type")
    val translations: TreeSet<TranslationDTO>,
    @field:Schema(description = "List of language codes supported by translations")
    val supportedLanguages: TreeSet<String>,
    @field:Schema(
        description = "The id of the resource resource type connection",
        example = "urn:resource-resourcetype:1",
    )
    val connectionId: URI,
) : Comparable<ResourceTypeWithConnectionDTO> {
  constructor(
      nodeId: URI,
      resourceType: ResourceType,
      languageCode: String,
  ) : this(
      id = resourceType.publicId,
      order = resourceType.order,
      translations = resourceType.translations.map(::TranslationDTO).toCollection(TreeSet()),
      supportedLanguages = TreeSet(resourceType.supportedLanguages),
      parentId = resourceType.parent?.publicId,
      name = resourceType.getTranslatedName(languageCode),
      connectionId = URI.create("urn:resource-resourcetype:${nodeId}_${resourceType.publicId}"),
  )

  override fun compareTo(other: ResourceTypeWithConnectionDTO) = order.compareTo(other.order)
}
