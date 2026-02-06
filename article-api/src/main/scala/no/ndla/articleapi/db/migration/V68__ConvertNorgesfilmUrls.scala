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

class V68__ConvertNorgesfilmUrls extends HtmlMigration {
  override val convertVisualElement: Boolean                        = true
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("ndlaembed[data-resource='iframe'][data-url^='https://ndla.filmiundervisning.no']")
      .forEach(embed => {
        val url = embed.attr("data-url")
        embed.attr("data-url", convertNorgesfilmUrl(url)): Unit
      })
    doc
      .select("a[href^='https://ndla.filmiundervisning.no']")
      .forEach(link => {
        val url = link.attr("href")
        link.attr("href", convertNorgesfilmUrl(url)): Unit
      })
    doc
  }

  private def convertNorgesfilmUrl(url: String): String = {
    url.replace("ndla.", "ndla2.").replace("/ndlafilm.aspx?filmId=", "/")
  }
}
