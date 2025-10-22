/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import no.ndla.testbase.UnitTestSuiteBase

class LanguageCodeTest extends UnitTestSuiteBase {
  test("That encoding LanguageCode works as expected") {
    import io.circe.syntax.*
    import io.circe.Encoder
    import io.circe.generic.semiauto.deriveEncoder

    case class ApiObject(normalField: String, languageField: LanguageCode)

    object ApiObject {
      implicit val encoder: Encoder[ApiObject] = deriveEncoder
    }

    val x = ApiObject("hei", LanguageCode("nb"))

    val res1 = x.asJson.noSpaces

    res1 should be("""{"normalField":"hei","languageField":"nb"}""")
  }

  test("That decoding LanguageCode works as expected") {
    import io.circe.Decoder
    import io.circe.generic.semiauto.deriveDecoder
    import io.circe.parser.parse

    case class ApiObject(normalField: String, languageField: LanguageCode)

    object ApiObject {
      implicit val decoder: Decoder[ApiObject] = deriveDecoder
    }

    val json   = """{"normalField":"hei","languageField":"nb"}"""
    val result = parse(json).flatMap(_.as[ApiObject])

    result should be(Right(ApiObject("hei", LanguageCode("nb"))))
  }
}
