/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V60__ConvertNorgesfilmUrlsTest extends UnitSuite with TestEnvironment {
  test("That norgesfilm urls looses ndlafilm.aspx") {
    val migration = new V60__ConvertNorgesfilmUrls
    val oldArticle =
      """<section><ndlaembed data-resource="iframe" data-type="iframe" data-url="https://ndla.filmiundervisning.no/film/ndlafilm.aspx?filmId=13074" data-width="700" data-height="300"></ndlaembed></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="iframe" data-type="iframe" data-url="https://ndla.filmiundervisning.no/film/13074" data-width="700" data-height="300"></ndlaembed></section>"""

    migration.convertContent(oldArticle, "nb") should be(newArticle)
  }
}
