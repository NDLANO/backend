/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import no.ndla.taxonomy.domain.Metadata

@Schema(name = "Metadata", requiredProperties = ["grepCodes", "visible", "customFields"])
data class MetadataDTO(
    @field:Schema var grepCodes: Set<String>,
    @field:Schema @get:JvmName("isVisible") var visible: Boolean,
    @field:Schema var customFields: Map<String, String>,
) {
  constructor(
      metadata: Metadata
  ) : this(
      grepCodes = metadata.getGrepCodes().mapTo(mutableSetOf()) { it.code },
      visible = metadata.isVisible(),
      customFields = metadata.getCustomFields(),
  )
}
