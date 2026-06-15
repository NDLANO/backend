/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.domain.exceptions.NotFoundException

@Schema(name = "ResourceResourceType", requiredProperties = ["id", "resourceId", "resourceTypeId"])
data class ResourceResourceTypeDTO(
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Resource type id",
        example = "urn:resource:123",
    )
    val resourceId: URI,
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Resource type id",
        example = "urn:resourcetype:234",
    )
    val resourceTypeId: URI,
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Resource to resource type connection id",
        example = "urn:resource-resourcetype:urn:resource:123_urn:resourcetype:subjectMaterial",
    )
    val id: URI,
) {
  constructor(
      resourceId: URI,
      resourceTypeId: URI,
  ) : this(
      id = URI.create("urn:resource-resourcetype:${resourceId}_$resourceTypeId"),
      resourceId = resourceId,
      resourceTypeId = resourceTypeId,
  )

  companion object {
    fun parseConnectionId(connectionId: URI): Pair<URI, URI> {
      try {
        val parts = connectionId.toString().removePrefix("urn:resource-resourcetype:").split("_")
        require(parts.size == 2) { "Invalid connection id: $connectionId" }
        return Pair(URI.create(parts[0]), URI.create(parts[1]))
      } catch (_: Exception) {
        throw NotFoundException("resource-resourcetype", connectionId)
      }
    }
  }
}
