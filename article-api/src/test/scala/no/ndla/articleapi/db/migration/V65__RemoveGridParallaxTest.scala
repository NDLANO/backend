/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V65__RemoveGridParallaxTest extends UnitSuite with TestEnvironment {
  test("That data-parallax-cell are removed from grid cells") {
    val migration = new V65__RemoveGridParallax
    val oldHtml   = """<div class="grid">
        |  <div data-type="grid-cell" data-parallax-cell="true">Content 1</div>
        |  <div data-type="grid-cell" data-parallax-cell="true">Content 2</div>
        |  <div data-type="grid-cell" data-parallax-cell="false">Content 3</div>
        |</div>""".stripMargin
    val newHtml = """<div class="grid">
        |  <div data-type="grid-cell">Content 1</div>
        |  <div data-type="grid-cell">Content 2</div>
        |  <div data-type="grid-cell">Content 3</div>
        |</div>""".stripMargin

    migration.convertContent(oldHtml, "nb") should be(newHtml)
  }
}
