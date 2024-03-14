/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model

import no.ndla.common.CirceUtil
import no.ndla.scalatestsuite.UnitTestSuite

class SearchableLanguageValuesTest extends UnitTestSuite {

  test("That serialization and deserialization results in same object") {
    import io.circe.syntax._
    val searchableLanguageValues = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Norsk"),
        LanguageValue("nn", "Nynorsk"),
        LanguageValue("en", "English")
      )
    )

    val jsonStr      = searchableLanguageValues.asJson.noSpaces
    val deserialized = CirceUtil.unsafeParseAs[SearchableLanguageValues](jsonStr)
    deserialized should be(searchableLanguageValues)
  }

  test("That serialization results in object with language as key") {
    import io.circe.syntax._
    val searchableLanguageValues = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Norsk"),
        LanguageValue("nn", "Nynorsk"),
        LanguageValue("en", "English")
      )
    )

    val json         = searchableLanguageValues.asJson.noSpaces
    val expectedJson = """{"nb":"Norsk","nn":"Nynorsk","en":"English"}"""
    json should be(expectedJson)
  }

}
