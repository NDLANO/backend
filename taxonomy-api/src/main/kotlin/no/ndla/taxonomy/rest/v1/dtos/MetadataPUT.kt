/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Optional

@Schema(name = "MetadataPUT")
class MetadataPUT {
  @field:JsonProperty
  @field:Schema(description = "Set of grep codes, Only updated if present")
  var grepCodes: Optional<Set<String>> = Optional.empty()

  @field:JsonProperty
  @field:Schema(description = "Visibility of the node, Only updated if present")
  var visible: Optional<Boolean> = Optional.empty()

  @field:JsonProperty
  @field:Schema(description = "Custom fields, Only updated if present")
  var customFields: Optional<Map<String, String>> = Optional.empty()
}
