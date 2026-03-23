/*
 * Part of NDLA draft-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.db.HtmlMigration
import org.jsoup.nodes.Element

class V83__UnwrapNestedMathTags extends HtmlMigration {
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
