/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI

@Schema
data class UrlMapping(
    @field:Schema(
        description = "URL for resource in old system",
        example = "ndla.no/nb/node/183926?fag=127013",
    )
    val url: String,
    @field:Schema(
        description = "Node URN for resource in new system",
        example = "urn:topic:1:183926",
    )
    val nodeId: String,
    @field:Schema(
        description = "Subject URN for resource in new system (optional)",
        example = "urn:subject:5",
    )
    val subjectId: String?,
) {
  constructor(
      url: String,
      nodeId: URI,
      subjectId: URI?,
  ) : this(
      url = url,
      nodeId = nodeId.toString(),
      subjectId = subjectId?.toString(),
  )
}
