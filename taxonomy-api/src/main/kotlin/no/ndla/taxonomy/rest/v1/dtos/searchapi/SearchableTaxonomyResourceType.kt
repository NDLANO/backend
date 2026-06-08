/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.searchapi

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.collections.buildMap
import no.ndla.taxonomy.config.Constants.DefaultLanguage
import no.ndla.taxonomy.domain.ResourceType

@Schema(requiredProperties = ["id", "name"])
data class SearchableTaxonomyResourceType(
    val id: String,
    val parentId: String? = null,
    val name: Map<String, String>,
    val order: Int,
) : Comparable<SearchableTaxonomyResourceType> {
  constructor(
      rt: ResourceType
  ) : this(
      id = rt.publicId.toString(),
      order = rt.order,
      parentId = rt.parent?.publicId?.toString(),
      name =
          buildMap<String, String> {
            put(DefaultLanguage, rt.name)
            rt.translations.forEach { t ->
              val code = t.languageCode ?: return@forEach
              val tName = t.name ?: return@forEach
              put(code, tName)
            }
          },
  )

  override fun compareTo(other: SearchableTaxonomyResourceType): Int {
    if (order == -1 || other.order == -1) {
      return id.compareTo(other.id)
    }
    return order - other.order
  }
}
