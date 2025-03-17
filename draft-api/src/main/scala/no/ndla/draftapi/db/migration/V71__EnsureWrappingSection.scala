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

class V71__EnsureWrappingSection extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    if (doc.select("body > section").isEmpty) {
      val body    = doc.select("body").first()
      val section = new Element("section").appendChildren(body.children())
      body.appendChild(section)
      doc
    } else {
      doc
    }
  }
}
