/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db

import io.circe.parser
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.draft.Draft
import no.ndla.database.DocumentMigration
import no.ndla.database.MigrationUtility.{jsoupDocumentToString, stringToJsoupDocument}
import org.jsoup.nodes.Element

abstract class HtmlMigration extends DocumentMigration {
  override val tableName: String  = "articledata"
  override val columnName: String = "document"

  /** Method to override that manipulates the content string */
  def convertHtml(doc: Element, language: String): Element
  val convertVisualElement: Boolean = false

  def convertContent(htmlString: String, language: String): String = {
    val doc       = stringToJsoupDocument(htmlString)
    val converted = convertHtml(doc, language)
    jsoupDocumentToString(converted)
  }

  def convertColumn(document: String): String = {
    val oldArticle       = parser.parse(document).flatMap(_.as[Draft]).toTry.get
    val convertedContent = oldArticle.content.map(c => {
      val converted = convertContent(c.content, c.language)
      c.copy(content = converted)
    })

    val convertedVisualElement = if (convertVisualElement) {
      oldArticle.visualElement.map(ve => {
        val doc       = stringToJsoupDocument(ve.resource)
        val converted = convertHtml(doc, ve.language)
        ve.copy(resource = jsoupDocumentToString(converted))
      })
    } else oldArticle.visualElement

    val newArticle = oldArticle.copy(content = convertedContent, visualElement = convertedVisualElement)
    newArticle.asJson.noSpaces
  }
}
