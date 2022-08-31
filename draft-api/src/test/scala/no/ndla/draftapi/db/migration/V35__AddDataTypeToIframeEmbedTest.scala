/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.db.migration.V35__AddDataTypeToIframeEmbed
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V35__AddDataTypeToIframeEmbedTest extends UnitSuite with TestEnvironment {

  test("Iframes in visualElement and content should be converted correctly") {

    val beforeArticle =
      """{"visualElement":[{"resource":"<embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\">","language":"nb"},{"resource":"<embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\">","language":"nn"}],"content":[{"content":"<section><embed data-url=\"https://statisk.ndla.no/applikasjon/nb\" data-title=\"Applikasjon\" data-caption=\"En fin applikasjon\" data-imageid=\"5432\" data-type=\"block\" data-resource=\"iframe\"><embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\"></section>","language":"nb"},{"content":"<section><embed data-url=\"https://statisk.ndla.no/applikasjon/nn\" data-title=\"Applikasjon\" data-caption=\"Ein fin applikasjon\" data-imageid=\"5432\" data-type=\"block\" data-resource=\"iframe\"><embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\"></section>","language":"nn"}]}"""
    val expectedArticle =
      """{"visualElement":[{"resource":"<embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\" data-type=\"iframe\">","language":"nb"},{"resource":"<embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\" data-type=\"iframe\">","language":"nn"}],"content":[{"content":"<section><embed data-url=\"https://statisk.ndla.no/applikasjon/nb\" data-title=\"Applikasjon\" data-caption=\"En fin applikasjon\" data-imageid=\"5432\" data-type=\"block\" data-resource=\"iframe\"><embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\" data-type=\"iframe\"></section>","language":"nb"},{"content":"<section><embed data-url=\"https://statisk.ndla.no/applikasjon/nn\" data-title=\"Applikasjon\" data-caption=\"Ein fin applikasjon\" data-imageid=\"5432\" data-type=\"block\" data-resource=\"iframe\"><embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-resource=\"iframe\" data-type=\"iframe\"></section>","language":"nn"}]}"""

    val migration = new V35__AddDataTypeToIframeEmbed
    migration.convertArticleUpdate(beforeArticle) should be(expectedArticle)
  }

  test("Already converted content should not be broken") {

    val beforeArticle =
      """{"visualElement":[{"resource":"<embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-type=\"iframe\" data-resource=\"iframe\">","language":"nb"},{"resource":"<embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-type=\"iframe\" data-resource=\"iframe\">","language":"nn"}],"content":[{"content":"<section><embed data-url=\"https://statisk.ndla.no/applikasjon/nb\" data-title=\"Applikasjon\" data-caption=\"En fin applikasjon\" data-imageid=\"5432\" data-type=\"block\" data-resource=\"iframe\"><embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-type=\"iframe\" data-resource=\"iframe\"></section>","language":"nb"},{"content":"<section><embed data-url=\"https://statisk.ndla.no/applikasjon/nn\" data-title=\"Applikasjon\" data-caption=\"Ein fin applikasjon\" data-imageid=\"5432\" data-type=\"block\" data-resource=\"iframe\"><embed data-width=\"800\" data-height=\"600\" data-url=\"https://youtu.be/videoid\" data-type=\"iframe\" data-resource=\"iframe\"></section>","language":"nn"}]}"""

    val migration = new V35__AddDataTypeToIframeEmbed
    migration.convertArticleUpdate(beforeArticle) should be(beforeArticle)
  }
}
