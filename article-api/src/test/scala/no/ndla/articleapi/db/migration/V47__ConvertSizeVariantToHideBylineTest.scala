/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.ArticleContent

class V47__ConvertSizeVariantToHideBylineTest extends UnitSuite with TestEnvironment {

  test("Migrate size attribute to hide caption and byline") {
    val migration = new V47__ConvertSizeVariantToHideByline
    val oldArticle =
      """<section><ndlaembed data-resource="image" data-resource_id="1" data-align="left" data-caption="Caption" data-size="full-hide-byline"></ndlaembed></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="image" data-resource_id="1" data-align="left" data-caption="Caption" data-size="full-hide-byline" data-hide-byline="true" data-hide-caption="true"></ndlaembed></section>"""

    val result: ArticleContent = migration.convertContent(ArticleContent(content = oldArticle, language = "nb"))
    result.content should equal(newArticle)
  }
}
