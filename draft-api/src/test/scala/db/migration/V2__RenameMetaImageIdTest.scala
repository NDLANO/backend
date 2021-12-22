/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V2__RenameMetaImageIdTest extends UnitSuite with TestEnvironment {
  val migration = new V2__RenameMetaImageId

  test("migration should remove the metaImageId field and add an metaImage array of one object") {
    val before = """{"metaImageId":"123","title":[{"title":"tittel","language":"nb"}]}"""
    val expected = """{"metaImage":[{"imageId":"123","language":"nb"}],"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(before) should equal(expected)
  }

  test("migration not do anyhting if the document is already is converted") {
    val original = """{"metaImage":[{"imageId":"123","language":"nb"}],"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(original) should equal(original)
  }

}
