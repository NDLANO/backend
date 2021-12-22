/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V10__UpdateLicesnsesTest extends UnitSuite with TestEnvironment {
  val migration = new V10__UpdateLicenses

  test("migration should update to new status format") {
    {
      val old =
        s"""{"copyright":{"license":"by","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":"by-sa","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(old) should equal(expected)

    }
    {
      val old =
        s"""{"copyright":{"license":"by-nc-nd","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-NC-ND-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":"copyrighted","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"COPYRIGHTED","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":"cc0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertArticleUpdate(old) should equal(expected)
    }
  }

  test("migration not do anything if the document already has new status format") {
    val original =
      s"""{"copyright":{"license":"COPYRIGHTED","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(original) should equal(original)
  }
}
