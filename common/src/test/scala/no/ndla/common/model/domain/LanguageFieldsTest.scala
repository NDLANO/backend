/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.language.*
import no.ndla.language.model.BaseWithLanguageAndValue
import no.ndla.scalatestsuite.UnitTestSuite

class LanguageFieldsTest extends UnitTestSuite {

  test("That language fields serialize and deserialize as expected") {
    import io.circe.syntax.*
    val fields = Seq(
      BaseWithLanguageAndValue("nb", "bokmål"),
      BaseWithLanguageAndValue("nn", "nynorsk"),
      BaseWithLanguageAndValue("en", "english")
    )

    val languageFields = LanguageFields.fromFields(fields)
    val jsonString     = languageFields.asJson.noSpaces
    val result         = CirceUtil.unsafeParseAs[LanguageFields[String]](jsonString)

    result should be(languageFields)
  }

  test("That language fields are found by language or best effort according to language priority") {
    val fields = Seq(
      BaseWithLanguageAndValue("nb", "bokmål"),
      BaseWithLanguageAndValue("nn", "nynorsk"),
      BaseWithLanguageAndValue("en", "english")
    )

    val languageFields = LanguageFields.fromFields(fields)

    languageFields.findByLanguageOrBestEffort("nb") should be(Some(BaseWithLanguageAndValue("nb", "bokmål")))
    languageFields.findByLanguageOrBestEffort("sma") should be(Some(BaseWithLanguageAndValue("nb", "bokmål")))
    languageFields.findByLanguageOrBestEffort("nn") should be(Some(BaseWithLanguageAndValue("nn", "nynorsk")))
  }

  test("That the LanguageFields type is able to differentiate between a missing and not needed field") {

    val fields = Seq(
      BaseWithLanguageAndValue[OptionalLanguageValue[String]]("nb", Exists("bokmål")),
      BaseWithLanguageAndValue[OptionalLanguageValue[String]]("nn", NotWanted())
    )

    val languageFields = LanguageFields.fromFields(fields)
    val jsonString     = CirceUtil.toJsonString(languageFields)

    val result = CirceUtil.unsafeParseAs[LanguageFields[OptionalLanguageValue[String]]](jsonString)
    result should be(languageFields)

    result.get("nb") should be(Some(BaseWithLanguageAndValue("nb", Exists("bokmål"))))
    result.get("nn") should be(Some(BaseWithLanguageAndValue("nn", NotWanted())))
  }

  test("That the OptLanguageFields type is able to differentiate between a missing and not needed field") {

    val fields = Seq(
      BaseWithLanguageAndValue[String]("nb", "bokmål")
    )

    val languageFields = OptLanguageFields.fromFields(fields).withUnwanted("en")
    val jsonString     = CirceUtil.toJsonString(languageFields)

    val result = CirceUtil.unsafeParseAs[OptLanguageFields[String]](jsonString)
    result should be(languageFields)

    result.get("nb") should be(Some(Exists("bokmål")))
    result.get("nn") should be(None)
    result.get("en") should be(Some(NotWanted()))
  }

}
