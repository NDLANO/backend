/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.util.Optional
import kotlin.jvm.optionals.getOrNull

data class GradeAverage(val averageSum: Int, val count: Int) {
  val averageValue: Double
    get() = averageSum.toDouble() / count

  companion object {
    fun fromGrades(grades: Collection<Optional<Grade>>): GradeAverage {
      val existing = grades.mapNotNull { it.getOrNull() }
      return GradeAverage(existing.sumOf { it.toInt() }, existing.size)
    }
  }
}
