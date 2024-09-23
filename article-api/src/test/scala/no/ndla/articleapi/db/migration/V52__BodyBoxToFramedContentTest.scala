/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.ArticleContent

class V52__BodyBoxToFramedContentTest extends UnitSuite with TestEnvironment {
  test("That body boxes are converted to framed content") {
    val oldDocument =
      """<section><div class="c-bodybox"><h3>Jadda</h3></div></section>"""
    val expectedDocument =
      """<section><div data-type="framed-content"><h3>Jadda</h3></div></section>"""

    val migration = new V52__BodyBoxToFramedContent
    val result    = migration.convertContent(oldDocument, "nb")
    result should be(expectedDocument)
  }

  test("That body boxes are converted to framed content even with other classes") {
    val oldDocument =
      """<section><div class="c-bodybox apekatt"><h3>Jadda</h3></div></section>"""
    val expectedDocument =
      """<section><div class="apekatt" data-type="framed-content"><h3>Jadda</h3></div></section>"""

    val migration = new V52__BodyBoxToFramedContent
    val result    = migration.convertContent(oldDocument, "nb")
    result should be(expectedDocument)
  }
}
