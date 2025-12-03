/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V80__RemoveGridBackgroundTest extends UnitSuite with TestEnvironment {
  val migration = new V80__RemoveGridBackground
  test("That data-background is removed from grid") {
    val oldHtml = """<div data-type="grid" data-background="transparent">
        |  <div data-type="grid-cell">Content 1</div>
        |  <div data-type="grid-cell">Content 2</div>
        |  <div data-type="grid-cell">Content 3</div>
        |</div>""".stripMargin
    val newHtml = """<div data-type="grid">
        |  <div data-type="grid-cell">Content 1</div>
        |  <div data-type="grid-cell">Content 2</div>
        |  <div data-type="grid-cell">Content 3</div>
        |</div>""".stripMargin

    migration.convertContent(oldHtml, "nb") should be(newHtml)
  }
}
