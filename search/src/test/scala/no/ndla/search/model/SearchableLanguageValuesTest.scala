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

  test("That SearchableLanguageValues serialization and deserialization results in same object") {
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

  test("That SearchableLanguageValues serialization results in object with language as key") {
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


  test("That SearchableLanguageList serialization and deserialization results in same object") {
    import io.circe.syntax._
    val searchableLanguageList = SearchableLanguageList(
      Seq(
        LanguageValue("nb", List("Norsk", "Norskere", "Norskest")),
        LanguageValue("nn", List("Nynorsk", "Nynorskere", "Nynorskest")),
        LanguageValue("en", List("English", "Englisher", "Englishest"))
      )
    )

    val jsonStr      = searchableLanguageList.asJson.noSpaces
    val deserialized = CirceUtil.unsafeParseAs[SearchableLanguageList](jsonStr)
    deserialized should be(searchableLanguageList)
  }

  test("That SearchableLanguageList serialization results in object with language as key") {
    import io.circe.syntax._
    val searchableLanguageList = SearchableLanguageList(
      Seq(
        LanguageValue("nb", List("Norsk", "Norskere", "Norskest")),
        LanguageValue("nn", List("Nynorsk", "Nynorskere", "Nynorskest")),
        LanguageValue("en", List("English", "Englisher", "Englishest"))
      )
    )

    val json         = searchableLanguageList.asJson.noSpaces
    val expectedJson = """{"nb":["Norsk","Norskere","Norskest"],"nn":["Nynorsk","Nynorskere","Nynorskest"],"en":["English","Englisher","Englishest"]}"""
    json should be(expectedJson)
  }

}
