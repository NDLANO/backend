/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class ResolvedOldUrl(
    @field:Schema(
        description = "URL path for resource",
        example = "'/subject:1/topic:12/resource:12'",
    )
    val path: String
)
