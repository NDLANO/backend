/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V5__MigrateStatusToQualityAssuredTest extends UnitSuite with TestEnvironment {
  val migration = new V5__MigrateStatusToQualityAssured

  test("migrations should rename status") {
    val old =
      s"""{"id":1,"title":{"title":"Test","language":"nb"},"content":{"content":"test.","language":"nb"},"supportedLanguages":["nb"],"status":{"current":"QUEUED_FOR_PUBLISHING","other":["PUBLISHED"]},"lastUpdated":"2018-05-11T16:02:40Z"}"""
    val expected =
      s"""{"id":1,"title":{"title":"Test","language":"nb"},"content":{"content":"test.","language":"nb"},"supportedLanguages":["nb"],"status":{"current":"QUALITY_ASSURED","other":["PUBLISHED"]},"lastUpdated":"2018-05-11T16:02:40Z"}"""
    migration.convertToNewConcept(old) should be(expected)
  }
}
