/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V11__RemoveEmptyStringMetaImagesTest extends UnitSuite with TestEnvironment {
  val migration = new V11__RemoveEmptyStringMetaImages

  private val metaImage = (id: String, alt: String, lang: String) => {
    s"""{"imageId":"$id","altText":"$alt","language":"$lang"}"""
  }

  test("MetaImages without ids should be removed") {
    val old =
      s"""{"metaImage":[${metaImage("", "alt", "nb")},${metaImage("1", "", "en")},${metaImage(
          "",
          "",
          "nn"
        )},${metaImage("2", "alt", "zh")}]}"""
    val expected =
      s"""{"metaImage":[${metaImage("1", "", "en")},${metaImage("2", "alt", "zh")}]}"""

    migration.convertToNewConcept(old) should be(expected)
  }

}
