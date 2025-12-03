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

class V66__RemoveGridBackground extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("div[data-type='grid'][data-background]")
      .forEach(embed => {
        val _ = embed.removeAttr("data-background")
      })
    doc
  }
}
