/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V79__RemoveGridParallaxTest extends UnitSuite with TestEnvironment {
  val migration = new V79__RemoveGridParallax
  test("That data-parallax-cell are removed from grid cells") {
    val oldHtml = """<div class="grid">
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
