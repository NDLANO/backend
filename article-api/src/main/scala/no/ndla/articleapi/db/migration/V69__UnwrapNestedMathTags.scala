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

  private def findOutermostMath(element: Element): Element = findOutermostMath(element.parent(), element)

  @annotation.tailrec
  private def findOutermostMath(current: Element, outermostMath: Element): Element = {
    if (current == null) outermostMath
    else if (current.normalName() == "math") findOutermostMath(current.parent(), current)
    else findOutermostMath(current.parent(), outermostMath)
  }
}
