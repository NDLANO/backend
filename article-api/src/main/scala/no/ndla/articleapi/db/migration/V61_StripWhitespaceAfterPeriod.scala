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

/** This migration is renamed to avoid running it before behaviour is changed in editor.
  */
class V61_StripWhitespaceAfterPeriod extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    if (doc.select("body").text() == doc.text()) {
      stripWhitespaceAfterPeriod(doc)
    }
    doc
      .select("p")
      .forEach(paragraph => {
        stripWhitespaceAfterPeriod(paragraph)
      })
    doc
  }

  private def stripWhitespaceAfterPeriod(element: Element): Unit = {
    val html = element.html()
    if (html.matches(""".*\.\s$""")) {
      // Remove whitespace after the last period in the HTML, only if it's at the end
      val updatedHtml = html.replaceFirst("""(\.\s+)(</?\w+.*?>)*\s*$""", ".$2")
      element.html(updatedHtml): Unit
    }

  }
}
