/*
 * Part of NDLA article-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V69__UnwrapNestedMathTags extends HtmlMigration {
  override def convertHtml(doc: Element, language: String): Element = {
    doc
      .select("math math")
      .forEach(nestedMath => {
        val outermostMath = findOutermostMath(nestedMath)
        if (nestedMath ne outermostMath) {
          nestedMath
            .attributes()
            .forEach(attribute => {
              if (!outermostMath.hasAttr(attribute.getKey)) {
                outermostMath.attr(attribute.getKey, attribute.getValue): Unit
              }
            })
          nestedMath.unwrap(): Unit
        }
      })
    doc
  }

  @annotation.tailrec
  private def findOutermostMath(element: Element): Element = {
    val parent = element.parent()
    if (parent != null && parent.normalName() == "math") findOutermostMath(parent)
    else element
  }
}
