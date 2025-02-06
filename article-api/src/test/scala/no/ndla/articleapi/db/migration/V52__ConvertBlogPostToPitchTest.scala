/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V52__ConvertBlogPostToPitchTest extends UnitSuite with TestEnvironment {
  test("That blog posts are converted to pitch embeds") {
    val oldArticle =
      """<section><ndlaembed data-resource="blog-post" data-title="Whatabout this" data-author="Some Author" data-size="normal" data-image-id="123" data-url="https://somewebsite.com" data-alt="Alternative"></ndlaembed></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="pitch" data-title="Whatabout this" data-image-id="123" data-url="https://somewebsite.com" data-alt="Alternative" data-description=""></ndlaembed></section>"""

    val migration = new V52__ConvertBlogPostToPitch
    val result    = migration.convertContent(oldArticle, "nb")
    result should be(newArticle)
  }

}
