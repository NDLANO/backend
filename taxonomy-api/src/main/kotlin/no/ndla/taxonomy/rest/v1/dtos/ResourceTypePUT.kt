/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.domain.ResourceType

@Schema
data class ResourceTypePUT(
    @field:Schema(
        description =
            "If specified, the new resource type will be a child of the mentioned resource type.")
    val parentId: URI? = null,
    @field:Schema(
        description =
            "If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.",
        example = "urn:resourcetype:1",
    )
    val id: URI? = null,
    @field:Schema(
        description = "The name of the resource type",
        example = "Lecture",
    )
    val name: String? = null,
    @field:Schema(
        description = "Order in which the resource type should be sorted among its siblings",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val order: Int = -1,
) {
  fun apply(entity: ResourceType) {
    // TODO: Is this a bug, or is it intended? We can null out the name here
    entity.name = name
    if (order > -1) {
      entity.order = order
    }
  }
}
