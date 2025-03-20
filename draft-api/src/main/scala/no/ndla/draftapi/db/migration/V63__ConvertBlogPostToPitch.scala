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

class V63__ConvertBlogPostToPitch extends HtmlMigration {

  /** Method to override that manipulates the content string */
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
