/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V56__DisclaimerToLanguageFieldsTest extends UnitSuite with TestEnvironment {
  test("That old disclaimers are migrated to new language fields") {
    val oldDocument =
      """{"disclaimer":[{"disclaimer":"Dette er bokmål","language":"nb"},{"disclaimer":"Dette er nynorsk","language":"nn"}]}"""
    val expectedResult = """{"disclaimer":{"nb":"Dette er bokmål","nn":"Dette er nynorsk"}}"""
    val migration      = new V56__DisclaimerToLanguageFields
    val result         = migration.convertColumn(oldDocument)
    result should be(expectedResult)
  }

  test("That no old disclaimers are migrated to new language fields") {
    val oldDocument    = """{}"""
    val expectedResult = """{"disclaimer":{}}"""
    val migration      = new V56__DisclaimerToLanguageFields
    val result         = migration.convertColumn(oldDocument)
    result should be(expectedResult)
  }

  test("That null disclaimers are migrated to new language fields") {
    val oldDocument    = """{"disclaimer":null}"""
    val expectedResult = """{"disclaimer":{}}"""
    val migration      = new V56__DisclaimerToLanguageFields
    val result         = migration.convertColumn(oldDocument)
    result should be(expectedResult)
  }
}
