/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI

@Schema(requiredProperties = ["resourceId", "resourceTypeId"])
data class ResourceResourceTypePOST(
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Resource id",
        example = "urn:resource:123",
    )
    val resourceId: URI,
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Resource type id",
        example = "urn:resourcetype:234",
    )
    val resourceTypeId: URI,
)
