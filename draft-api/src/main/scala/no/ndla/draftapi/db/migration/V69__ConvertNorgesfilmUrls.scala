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

class V69__ConvertNorgesfilmUrls extends HtmlMigration {
  override val convertVisualElement: Boolean = true
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("ndlaembed[data-resource='iframe']")
      .forEach(embed => {
        val url = embed.attr("data-url")
        if (url.contains("ndla.filmiundervisning.no/film/ndlafilm.aspx?filmId=")) {
          embed.attr("data-url", url.replace("/ndlafilm.aspx?filmId=", "/")): Unit
        }
      })
    doc
  }

}
