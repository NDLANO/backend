/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search.model

import io.circe.Decoder
import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.testbase.UnitTestSuiteBase

class SearchableLanguageValuesTest extends UnitTestSuiteBase {

  test("That SearchableLanguageValues serialization and deserialization results in same object") {
    import io.circe.syntax.*
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

  test("That no SearchableLanguageValues deserialization to something") {
    case class TestClass(someField: SearchableLanguageValues)
    implicit val decoder: Decoder[TestClass] = io.circe.generic.semiauto.deriveDecoder[TestClass]
    val expected                             = SearchableLanguageValues(Seq())

    val jsonStr      = """{}"""
    val deserialized = CirceUtil.unsafeParseAs[TestClass](jsonStr)
    deserialized should be(TestClass(expected))
  }

  test("That SearchableLanguageValues serialization results in object with language as key") {
    import io.circe.syntax.*
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
    import io.circe.syntax.*
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
    import io.circe.syntax.*
    val searchableLanguageList = SearchableLanguageList(
      Seq(
        LanguageValue("nb", List("Norsk", "Norskere", "Norskest")),
        LanguageValue("nn", List("Nynorsk", "Nynorskere", "Nynorskest")),
        LanguageValue("en", List("English", "Englisher", "Englishest"))
      )
    )

    val json         = searchableLanguageList.asJson.noSpaces
    val expectedJson =
      """{"nb":["Norsk","Norskere","Norskest"],"nn":["Nynorsk","Nynorskere","Nynorskest"],"en":["English","Englisher","Englishest"]}"""
    json should be(expectedJson)
  }

}
