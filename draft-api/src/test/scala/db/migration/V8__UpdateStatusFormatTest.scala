/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V8__UpdateStatusFormatTest extends UnitSuite with TestEnvironment {
  val migration = new V8__UpdateStatusFormat

  test("migration should update to new status format") {
    {
      val publishedImported = """{"status":["IMPORTED", "PUBLISHED"],"title":[{"title":"tittel","language":"nb"}]}"""
      val expectedPublishedImported =
        """{"status":{"current":"PUBLISHED","other":["IMPORTED"]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(publishedImported) should equal(expectedPublishedImported)
    }

    {
      val draftImported = """{"status":["IMPORTED", "DRAFT"],"title":[{"title":"tittel","language":"nb"}]}"""
      val expectedDraftImported =
        """{"status":{"current":"DRAFT","other":["IMPORTED"]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(draftImported) should equal(expectedDraftImported)
    }

    {
      val queuedForPublish = """{"status":["QUEUED_FOR_PUBLISHING"],"title":[{"title":"tittel","language":"nb"}]}"""
      val expectedQueuedForPublish =
        """{"status":{"current":"QUEUED_FOR_PUBLISHING","other":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(queuedForPublish) should equal(expectedQueuedForPublish)
    }

    {
      val before = """{"status":["CREATED", "IMPORTED"],"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        """{"status":{"current":"DRAFT","other":["IMPORTED"]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(before) should equal(expected)
    }
  }

  test("migration not do anyhting if the document already has new status format") {
    val original =
      """{"status":{"current":"PUBLISHED","other":["IMPORTED"]},"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(original) should equal(original)
  }

}
