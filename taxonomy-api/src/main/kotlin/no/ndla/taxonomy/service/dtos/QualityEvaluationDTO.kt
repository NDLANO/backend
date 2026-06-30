/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.jvm.optionals.getOrNull
import no.ndla.taxonomy.domain.Grade
import no.ndla.taxonomy.domain.Node

@Schema
data class QualityEvaluationDTO(
    @field:Schema(description = "The grade (1-5) of the article") val grade: Grade,
    @field:Schema(description = "Note explaining the score") val note: String?,
) {
  companion object {
    fun fromNode(node: Node): QualityEvaluationDTO? =
        node.qualityEvaluationGrade.getOrNull()?.let {
          QualityEvaluationDTO(it, node.qualityEvaluationNote.getOrNull())
        }
  }
}
