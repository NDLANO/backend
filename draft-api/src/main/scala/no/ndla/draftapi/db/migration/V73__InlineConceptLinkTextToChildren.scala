/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V73__InlineConceptLinkTextToChildren extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("ndlaembed[data-resource='concept'][data-link-text]")
      .forEach(embed => {
        val linkText = embed.attr("data-link-text")
        embed.append(linkText)
        val _ = embed.removeAttr("data-link-text")
      })
    doc
  }
}
