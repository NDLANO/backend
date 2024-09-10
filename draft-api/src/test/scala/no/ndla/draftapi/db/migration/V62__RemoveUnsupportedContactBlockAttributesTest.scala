/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.common.model.domain.ArticleContent
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V62__RemoveUnsupportedContactBlockAttributesTest extends UnitSuite with TestEnvironment {
  test("Migration should remove data-blob and data-blob-color from contact-block embeds") {
    val migration = new V62__RemoveUnsupportedContactBlockAttributes
    val oldArticle =
      """<section><ndlaembed data-resource="contact-block" data-email="example@example.com" data-job-title="Sjef" data-name="Sjef Sjefesen" data-image-id="123" data-blob="round" data-blob-color="green"></ndlaembed><ndlaembed data-resource="image" data-image-id="11"></ndlaembed><p>hallo sjef</p></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="contact-block" data-email="example@example.com" data-job-title="Sjef" data-name="Sjef Sjefesen" data-image-id="123"></ndlaembed><ndlaembed data-resource="image" data-image-id="11"></ndlaembed><p>hallo sjef</p></section>"""

    val result: ArticleContent = migration.convertContent(ArticleContent(content = oldArticle, language = "nb"))
    result.content should equal(newArticle)
  }
}