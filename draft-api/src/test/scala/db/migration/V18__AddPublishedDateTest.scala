/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V18__AddPublishedDateTest extends UnitSuite with TestEnvironment {
  val migration = new V18__AddPublishedDate

  test("article should get same published date as saved updated date") {
    val old =
      s"""{"metaDescription":[{"content":"what","language":"nb"}],"articleType":"standard","status":{"current":"PUBLISHED","other":[]},"updated":"2018-01-01T13:00:00Z"}"""
    val expected =
      s"""{"metaDescription":[{"content":"what","language":"nb"}],"articleType":"standard","status":{"current":"PUBLISHED","other":[]},"updated":"2018-01-01T13:00:00Z","published":"2018-01-01T13:00:00Z"}"""

    val result = migration.convertArticleUpdate(old)
    result.toString should be(expected)
  }
}
