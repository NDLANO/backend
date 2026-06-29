/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.jvm.optionals.getOrNull
import no.ndla.taxonomy.domain.Node

@Schema
data class TechnicalEvaluationDTO(
    @field:Schema(description = "Whether this node requires a technical evaluation.")
    val requiresEvaluation: Boolean,
    @field:Schema(description = "Notes for the technical evaluation of this node.")
    val comment: String? = null,
) {
  companion object {
    fun fromNode(node: Node) =
        node.requiresTechnicalEvaluation().getOrNull()?.let {
          TechnicalEvaluationDTO(it, node.technicalEvaluationComment.getOrNull())
        }
  }
}
