/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V45__RenameSearchStatusesTest extends UnitSuite with TestEnvironment {

  test("That converting content-links works as expected") {
    val testHtml =
      """{"userId":"userId","savedSearches":["/search/content?draft-status=DRAFT&fallback=true&language=nb&page=1&page-size=10&query=london&sort=-lastUpdated","/search/content?draft-status=QUALITY_ASSURED_DELAYED&fallback=true&language=nb&page=1&page-size=10&query=Berlin&sort=-lastUpdated"],"latestEditedArticles":["30892","4922","24252","120","21627","21996","20723","29564","27472","30228"]}"""
    val expectedResult =
      """{"userId":"userId","savedSearches":["/search/content?draft-status=PLANNED&fallback=true&language=nb&page=1&page-size=10&query=london&sort=-lastUpdated","/search/content?draft-status=END_CONTROL&fallback=true&language=nb&page=1&page-size=10&query=Berlin&sort=-lastUpdated"],"latestEditedArticles":["30892","4922","24252","120","21627","21996","20723","29564","27472","30228"]}"""
    val migration = new V45__RenameSearchStatuses
    val result    = migration.convertUser(testHtml)
    result should be(expectedResult)
  }
}
