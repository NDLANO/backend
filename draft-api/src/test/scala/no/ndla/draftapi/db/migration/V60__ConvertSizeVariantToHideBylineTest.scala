/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.common.model.domain.ArticleContent
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V60__ConvertSizeVariantToHideBylineTest extends UnitSuite with TestEnvironment {

  test("Migrate size attribute to hide caption and byline") {
    val migration = new V60__ConvertSizeVariantToHideByline
    val oldArticle =
      """<section><ndlaembed data-resource="image" data-resource_id="1" data-align="left" data-caption="Caption" data-size="full--hide-byline"></ndlaembed></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="image" data-resource_id="1" data-align="left" data-caption="Caption" data-size="full--hide-byline" data-hide-byline="true" data-hide-caption="true"></ndlaembed></section>"""

    val result: ArticleContent = migration.convertContent(ArticleContent(content = oldArticle, language = "nb"))
    result.content should equal(newArticle)
  }
}
