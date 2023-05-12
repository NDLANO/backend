/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import no.ndla.conceptapi.db.migration.V14__TrimConceptTitle
import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V14__TrimConceptTitleTest extends UnitSuite with TestEnvironment {
  val migration = new V14__TrimConceptTitle

  private val title = (title: String, lang: String) => s"""{"title":"$title","language":"$lang"}"""

  test("leading and trailing whitespaces should be removed from titles") {
    val old =
      s"""{"title":[${title("   3 leading spaces", "nb")},${title(
          "  2 leading and 3 trailing spaces   ",
          "en"
        )},${title("whitespace in       the middle", "nn")},${title("no whitespace", "zh")}]}"""
    val expected =
      s"""{"title":[${title("3 leading spaces", "nb")},${title("2 leading and 3 trailing spaces", "en")},${title(
          "whitespace in       the middle",
          "nn"
        )},${title("no whitespace", "zh")}]}"""

    migration.convertToNewConcept(old) should be(expected)
  }

}
