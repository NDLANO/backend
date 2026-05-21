/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.util.stream.Collectors
import no.ndla.taxonomy.domain.Metadata

@Schema(name = "Metadata", requiredProperties = ["grepCodes", "visible", "customFields"])
open class MetadataDTO {
  @field:Schema var grepCodes: Set<String>? = null

  @field:Schema @get:JvmName("isVisible") var visible: Boolean? = null

  @field:Schema var customFields: Map<String, String>? = null

  constructor()

  constructor(metadata: Metadata) {
    this.visible = metadata.isVisible()
    this.grepCodes = metadata.getGrepCodes().stream().map { it.code }.collect(Collectors.toSet())
    this.customFields = metadata.getCustomFields()
  }
}
