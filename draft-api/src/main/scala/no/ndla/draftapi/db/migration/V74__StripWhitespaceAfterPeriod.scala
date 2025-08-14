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

class V74__StripWhitespaceAfterPeriod extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("p")
      .forEach(paragraph => {
        val html = paragraph.html()
        if (html.matches(""".*\.\s$""")) {
          // Remove whitespace after the last period in the HTML, only if it's at the end
          val updatedHtml = html.replaceFirst("""(\.\s+)(</?\w+.*?>)*\s*$""", ".$2")
          paragraph.html(updatedHtml): Unit
        }
      })
    doc
  }
}
