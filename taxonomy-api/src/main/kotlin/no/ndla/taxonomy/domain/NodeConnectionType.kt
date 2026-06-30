/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class NodeConnectionType(val from: String, val to: String) {
  BRANCH("parent", "child"),
  LINK("referrer", "target"),
}
