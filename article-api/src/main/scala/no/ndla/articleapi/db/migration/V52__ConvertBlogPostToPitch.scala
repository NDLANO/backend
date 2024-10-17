/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V52__ConvertBlogPostToPitch extends HtmlMigration {
  override val convertVisualElement: Boolean = false
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("ndlaembed[data-resource='blog-post']")
      .forEach(embed => {
        embed.attr("data-resource", "pitch")
        embed.attr("data-description", "")
        embed.removeAttr("data-size"): Unit
        embed.removeAttr("data-author"): Unit
      })
    doc
  }
}
