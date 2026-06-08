/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.domain.ResourceResourceType

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
        example = "urn:resource-has-resourcetypes:12",
    )
    val id: URI,
) {
  constructor(
      rrt: ResourceResourceType
  ) : this(
      id = rrt.publicId,
      resourceId = rrt.node.publicId,
      resourceTypeId = rrt.resourceType.publicId,
  )
}
