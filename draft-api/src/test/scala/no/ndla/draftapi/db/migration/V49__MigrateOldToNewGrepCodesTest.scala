/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V49__MigrateOldToNewGrepCodesTest extends UnitSuite with TestEnvironment {

  test("That old grep codes are converted if in mapping otherwise left alone") {
    val oldDocument =
      """{"grepCodes":["KE152","KE1111"],"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"grepCodes":["KE1427","KE1111"],"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val migration = new V49__MigrateOldToNewGrepCodes
    val result    = migration.convertArticleUpdate(oldDocument)
    result should be(expectedDocument)
  }
}
