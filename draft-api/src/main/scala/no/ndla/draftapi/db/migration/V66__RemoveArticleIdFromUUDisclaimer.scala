/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V66__RemoveArticleIdFromUUDisclaimer extends HtmlMigration {
  override val convertVisualElement: Boolean = true
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("ndlaembed[data-resource='uu-disclaimer']")
      .forEach(embed => {
        val hasArticleId = embed.hasAttr("data-article-id")
        if (hasArticleId) {
          embed.removeAttr(
            "data-article-id"
          ): Unit
        }
      })
    doc
  }
}
