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

class V61__StripWhitespaceAfterPeriod extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("p")
      .forEach(paragraph => {
        val html = paragraph.html()
        val text = paragraph.text()
        if (text.matches(""".*\.\s$""")) {
          // Remove whitespace after the last period in the HTML, only if it's at the end
          val updatedHtml = html.replaceFirst("""(\.\s+)(</?\w+.*?>)*\s*$""", ".$2")
          paragraph.text(updatedHtml): Unit
        }
      })
    doc
  }
}
