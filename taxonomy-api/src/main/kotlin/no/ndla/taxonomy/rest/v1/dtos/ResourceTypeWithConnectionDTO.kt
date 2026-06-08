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
import no.ndla.taxonomy.domain.ResourceResourceType
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
      rrt: ResourceResourceType,
      languageCode: String,
  ) : this(
      id = rrt.resourceType.publicId,
      order = rrt.resourceType.order,
      translations = rrt.resourceType.translations.map(::TranslationDTO).toCollection(TreeSet()),
      supportedLanguages = rrt.resourceType.supportedLanguages,
      parentId = rrt.resourceType.parent?.publicId,
      name = rrt.resourceType.getTranslatedName(languageCode),
      connectionId = rrt.publicId,
  )

  override fun compareTo(other: ResourceTypeWithConnectionDTO): Int {
    if (order == -1 || other.order == -1) {
      return this.id.compareTo(other.id)
    }
    return this.order.compareTo(other.order)
  }
}
