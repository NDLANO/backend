/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "MetadataPUT")
data class MetadataPUT(
    @field:Schema(description = "Set of grep codes, Only updated if present")
    var grepCodes: Set<String>? = null,
    @field:Schema(description = "Visibility of the node, Only updated if present")
    var visible: Boolean? = null,
    @field:Schema(description = "Custom fields, Only updated if present")
    var customFields: Map<String, String>? = null,
)
