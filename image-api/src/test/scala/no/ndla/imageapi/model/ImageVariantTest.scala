/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model

import no.ndla.common.CirceUtil
import no.ndla.imageapi.model.domain.{ImageVariant, ImageVariantSize}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class ImageVariantTest extends UnitSuite, TestEnvironment {
  test("that ImageVariantSize successfully decodes from valid JSON") {
    val json    = "\"medium\""
    val decoded = CirceUtil.tryParseAs[ImageVariantSize](json).failIfFailure
    decoded should be(ImageVariantSize.Medium)
  }

  test("that ImageVariant successfully decodes from valid JSON") {
    val json = """
        |{
        |  "size": "medium",
        |  "bucketKey": "makkapakka"
        |}
        |""".stripMargin
    val expected = ImageVariant(ImageVariantSize.Medium, "makkapakka")
    val decoded  = CirceUtil.tryParseAs[ImageVariant](json).failIfFailure
    decoded should be(expected)
  }
}
