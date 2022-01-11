/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class R__MetaImageAsVisualElementTest extends UnitSuite with TestEnvironment {
  val migration = new R__MetaImageAsVisualElement

  private val metaImage = (id: String, alt: String, lang: String) => {
    s"""{"imageId":"$id","altText":"$alt","language":"$lang"}"""
  }

  test("Meta images without ids should not affect visual elements") {
    val oldMetaImage = """{"imageId":"","altText":"","language":"nb"}"""
    val oldVisualElement = """"""
    val old =
      s"""{"metaImage":[$oldMetaImage],"visualElement":[$oldVisualElement]}"""
    val expected = old
    migration.convertToNewConcept(old) should be(expected)
  }

  test("Meta images with ids should become visual elements") {
    val oldMetaImage = """{"imageId":"1","altText":"alt","language":"nb"}"""
    val oldVisualElement = """"""
    val old =
      s"""{"metaImage":[$oldMetaImage],"visualElement":[$oldVisualElement]}"""

    val visualElementString =
      s"""<embed data-resource=\\"image\\" data-resource_id=\\"1\\" data-alt=\\"alt\\" data-size=\\"full\\" data-align=\\"\\" />"""
    val expectedVisualElement = s"""{"visualElement":"$visualElementString","language":"nb"}"""

    val expected =
      s"""{"metaImage":[$oldMetaImage],"visualElement":[$expectedVisualElement]}"""
    migration.convertToNewConcept(old) should be(expected)
  }

}
