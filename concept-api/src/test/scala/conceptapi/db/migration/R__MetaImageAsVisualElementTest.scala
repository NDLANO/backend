/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package conceptapi.db.migration

import conceptapi.db.migration.R__MetaImageAsVisualElement
import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class R__MetaImageAsVisualElementTest extends UnitSuite with TestEnvironment {
  val migration = new R__MetaImageAsVisualElement

  test("Meta images without ids should not affect visual elements") {
    val oldMetaImage     = """{"imageId":"","altText":"","language":"nb"}"""
    val oldVisualElement = """"""
    val old =
      s"""{"metaImage":[$oldMetaImage],"visualElement":[$oldVisualElement]}"""
    val expected = old
    migration.convertToNewConcept(old) should be(expected)
  }

  test("Meta images with ids should become visual elements") {
    val oldMetaImage     = """{"imageId":"1","altText":"alt","language":"nb"}"""
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

  test("Meta images should not override existing visual elements") {
    val oldMetaImageNb = """{"imageId":"1","altText":"alt","language":"nb"}"""
    val oldMetaImageNn = """{"imageId":"1","altText":"alt2","language":"nn"}"""
    val oldVisualElementString =
      """<embed data-resource_id=\"123\" data-resource=\"image\" data-alt=\"somealt\" data-size=\"full\" data-align=\"\" />"""

    val oldVisualElement = s"""{"visualElement":"$oldVisualElementString","language":"nb"}"""

    val old =
      s"""{"metaImage":[$oldMetaImageNb,$oldMetaImageNn],"visualElement":[$oldVisualElement]}"""

    val newVisualElementString =
      """<embed data-resource=\"image\" data-resource_id=\"1\" data-alt=\"alt2\" data-size=\"full\" data-align=\"\" />"""

    val expectedNewVisualElement = s"""{"visualElement":"$newVisualElementString","language":"nn"}"""

    val expected =
      s"""{"metaImage":[$oldMetaImageNb,$oldMetaImageNn],"visualElement":[$oldVisualElement,$expectedNewVisualElement]}"""
    migration.convertToNewConcept(old) should be(expected)

  }

}
