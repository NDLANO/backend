/*
 * Part of NDLA draft-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V58__MigrateSavedSearchUrlsTest extends UnitSuite with TestEnvironment {
  val migration = new V58__MigrateSavedSearchUrls

  test("Migrations changes searchUrl and status parameters in saved searches") {
    val oldSearch = """{
                      |    "userId": "userid",
                      |    "savedSearches": [
                      |        {
                      |            "searchUrl": "/search/concept?exclude-revision-log=false&fallback=false&filter-inactive=true&include-other-statuses=false&page-size=10&sort=-lastUpdated&status=PUBLISHED",
                      |            "searchPhrase": "Forklaring + Publisert"
                      |        }
                      |    ],
                      |    "latestEditedArticles": [],
                      |    "latestEditedConcepts": [],
                      |    "favoriteSubjects": []
                      |}""".stripMargin
    val newSearch =
      """{"userId":"userid","savedSearches":[{"searchUrl":"/search/content?exclude-revision-log=false&fallback=false&filter-inactive=true&include-other-statuses=false&page-size=10&sort=-lastUpdated&draft-status=PUBLISHED","searchPhrase":"Forklaring + Publisert"}],"latestEditedArticles":[],"latestEditedConcepts":[],"favoriteSubjects":[]}"""
    val result = migration.convertDocument(oldSearch)

    result should equal(newSearch)
  }

}
