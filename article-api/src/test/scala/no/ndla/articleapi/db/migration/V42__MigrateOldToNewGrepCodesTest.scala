/*
 * Part of NDLA article-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V42__MigrateOldToNewGrepCodesTest extends UnitSuite with TestEnvironment {

  test("That old grep codes are converted if in mapping otherwise left alone") {
    val oldDocument =
      """{"grepCodes":["KE152","KE1111"],"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"grepCodes":["KE1427","KE1111"],"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val migration = new V42__MigrateOldToNewGrepCodes
    val result    = migration.convertArticleUpdate(oldDocument)
    result should be(expectedDocument)
  }
}
