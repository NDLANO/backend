/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToLong
import no.ndla.taxonomy.domain.Node

@Schema(name = "GradeAverage")
data class GradeAverageDTO(val averageValue: Double, val count: Int) {
  companion object {
    fun roundToSingleDecimal(value: Double): Double = (value * 10).roundToLong() / 10.0

    fun fromNode(node: Node): GradeAverageDTO? =
        node.childQualityEvaluationAverage.getOrNull()?.let {
          GradeAverageDTO(roundToSingleDecimal(it.averageValue), it.count)
        }
  }
}
