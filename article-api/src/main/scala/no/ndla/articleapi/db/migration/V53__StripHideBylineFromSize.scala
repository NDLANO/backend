/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V53__StripHideBylineFromSize extends HtmlMigration {

  override val convertVisualElement: Boolean                        = true
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("ndlaembed[data-resource='image']")
      .forEach(embed => {
        val hasSize = embed.hasAttr("data-size")
        if (hasSize) {
          if (embed.attr("data-size").contains("-")) {
            embed.attr("data-size", embed.attr("data-size").substring(0, embed.attr("data-size").indexOf("-"))): Unit
          }
          if (embed.attr("data-size").equals("fullbredde") || embed.attr("data-size").equals("fullwidth")) {
            embed.attr("data-size", "full"): Unit
          }
        }
      })
    doc
  }
}
