/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V10__RenameMetaImageIdTest extends UnitSuite with TestEnvironment {
  val migration = new V10__RenameMetaImageId

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
