/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import jakarta.persistence.AttributeConverter

class GradeConverter : AttributeConverter<Grade, Int> {
  override fun convertToDatabaseColumn(grade: Grade?): Int? = grade?.toInt()

  override fun convertToEntityAttribute(integer: Int?) = integer?.let { Grade.fromInt(it) }
}
