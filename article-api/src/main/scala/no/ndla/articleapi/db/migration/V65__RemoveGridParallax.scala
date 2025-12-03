/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V65__RemoveGridParallax extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("div[data-parallax-cell]")
      .forEach(embed => {
        val _ = embed.removeAttr("data-parallax-cell")
      })
    doc
  }
}
