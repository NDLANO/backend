/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.domain.Version

@Schema
data class VersionPut(
    @field:Schema(
        description =
            "If specified, set the id to this value. Must start with urn:version: and be a valid URI. If omitted, an id will be assigned automatically.",
        example = "urn:version:1",
    )
    val id: URI? = null,
    @field:Schema(
        description = "If specified, set the name to this value.",
        example = "Beta 2022",
    )
    val name: String?,
    @field:Schema(description = "If specified, set the locked property to this value")
    val locked: Boolean? = null,
) {
  fun apply(entity: Version) {
    id?.let { entity.publicId = it }
    name?.let { entity.name = it }
    locked?.let { entity.isLocked = it }
  }
}
