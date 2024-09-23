/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.common.model.domain.ArticleContent
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V63__RemoveConceptListEmbedsTest extends UnitSuite with TestEnvironment {
  test("Migration should remove concept-list embeds") {
    val migration = new V63__RemoveBlogPostEmbeds
    val oldArticle =
      """<section><ndlaembed data-resource="image" data-image-id="11"></ndlaembed><ndlaembed data-resource="blog-post" data-stuff="Hello" data-does-this-even-work="maybe"></ndlaembed><p>hallo sjef</p></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="image" data-image-id="11"></ndlaembed><p>hallo sjef</p></section>"""

    val result: ArticleContent = migration.convertContent(ArticleContent(content = oldArticle, language = "nb"))
    result.content should equal(newArticle)
  }
}
