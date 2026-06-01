/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.net.URI
import java.time.Instant
import no.ndla.taxonomy.domain.Version
import no.ndla.taxonomy.domain.VersionType

@Schema(
    name = "Version",
    requiredProperties = ["id", "versionType", "name", "hash", "locked", "created"],
)
data class VersionDTO(
    @field:Schema(example = "urn:version:1") val id: URI,
    @field:Schema(example = "BETA") @Enumerated(EnumType.STRING) val versionType: VersionType,
    @field:Schema(description = "Name for the version") val name: String,
    @field:Schema(description = "Unique hash for the version") val hash: String,
    @field:Schema(description = "Is the version locked") val locked: Boolean,
    @field:Schema(description = "Timestamp for when version was created") val created: Instant,
    @field:Schema(description = "Timestamp for when version was published") val published: Instant?,
    @field:Schema(description = "Timestamp for when version was archived") val archived: Instant?,
) {
  constructor(
      version: Version
  ) : this(
      id = version.publicId,
      versionType = version.versionType,
      name = version.name ?: "",
      hash = version.hash,
      locked = version.isLocked,
      created = version.created,
      published = version.published,
      archived = version.archived,
  )
}
