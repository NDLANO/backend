/*
 * Part of NDLA backend.article-api.main
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V52__BodyBoxToFramedContent extends HtmlMigration {
  override val convertVisualElement: Boolean = false
  override def convertHtml(html: Element, language: String): Element = {
    html
      .select("div.c-bodybox")
      .forEach(div => {
        div.removeClass("c-bodybox")
        div.attr("data-type", "framed-content"): Unit
      })
    html
  }
}
