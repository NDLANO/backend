/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Grade(val value: Int) {
  One(1),
  Two(2),
  Three(3),
  Four(4),
  Five(5),
  ;

  @JsonValue fun toInt() = value

  companion object {
    fun fromInt(value: Int): Grade =
        when (value) {
          1 -> One
          2 -> Two
          3 -> Three
          4 -> Four
          5 -> Five
          else -> throw IllegalArgumentException("Unexpected grade value: $value. Must be 1-5.")
        }
  }
}
