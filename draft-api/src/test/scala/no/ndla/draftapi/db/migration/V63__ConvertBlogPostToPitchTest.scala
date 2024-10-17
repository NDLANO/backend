/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V63__ConvertBlogPostToPitchTest extends UnitSuite with TestEnvironment {
  test("Migration should convert blog-post to pitch embeds") {
    val migration = new V63__ConvertBlogPostToPitch
    val oldArticle =
      """<section><ndlaembed data-resource="blog-post" data-title="Whatabout this" data-author="Some Author" data-size="normal" data-image-id="123" data-url="https://somewebsite.com" data-alt="Alternative"></ndlaembed></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="pitch" data-title="Whatabout this" data-image-id="123" data-url="https://somewebsite.com" data-alt="Alternative" data-description=""></ndlaembed></section>"""

    val result = migration.convertContent(oldArticle, language = "nb")
    result should equal(newArticle)
  }
}
