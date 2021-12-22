/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date
import java.util.UUID.randomUUID

import no.ndla.draftapi.model.domain.ArticleType
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JArray, JObject}
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{Extraction, Formats}
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.nodes.TextNode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.language.{implicitConversions, postfixOps}

class V17__MoveTopicArticleEmbedToVisualElement extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateTopicArticleEmbeds
    }
  }

  def migrateTopicArticleEmbeds(implicit session: DBSession): Unit = {
    val count = countAllTopicArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allTopicArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertTopicArticle(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllTopicArticles(implicit session: DBSession): Option[Long] = {
    sql"""select count(*) from articledata where document is not NULL and document@>'{"articleType":"topic-article"}'"""
      .map(rs => rs.long("count"))
      .single()
  }

  def allTopicArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"""
         select id, document from articledata
         where document is not null
         and document@>'{"articleType":"topic-article"}'
         order by id limit 1000 offset $offset
      """
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def newStatus(extractedArticle: V16__Article): V16__Status =
    if (extractedArticle.status.current == V16__ArticleStatus.PUBLISHED)
      extractedArticle.status.copy(current = V16__ArticleStatus.AWAITING_QUALITY_ASSURANCE)
    else extractedArticle.status

  def convertTopicArticle(document: String): String = {
    implicit val formats
      : Formats = org.json4s.DefaultFormats + new EnumNameSerializer(V16__ArticleStatus) + new EnumNameSerializer(
      ArticleType)

    val oldArticle = parse(document)
    val extractedArticle = oldArticle.extract[V16__Article]

    val contentWithExtractedEmbeds = extractedArticle.content.map(cont => {
      val (extractedEmbed, newContentString) = extractEmbedFromContent(cont.content)
      cont.copy(content = newContentString) -> extractedEmbed
    })

    val contentIsChanged = extractedArticle.content != contentWithExtractedEmbeds.map(_._1)

    if (contentIsChanged) {
      val newVisualElements = contentWithExtractedEmbeds.collect {
        case (content, Some(extractedEmbed))
            if !extractedArticle.visualElement.map(_.language).contains(content.language) =>
          V16__VisualElement(extractedEmbed, content.language)
      }

      val allVisualElements = extractedArticle.visualElement ++ newVisualElements
      val updatedStatus = newStatus(extractedArticle)
      val noteToAppend = V16__EditorNote(
        s"Embed plassert før første tekst har blitt slettet og gjort om til visuelt element, dersom det var mulig. Status har blitt endret til 'Til kvalitetssikring'.",
        "System",
        extractedArticle.status,
        new Date()
      )

      val updatedArticle = oldArticle
        .replace(List("visualElement"), Extraction.decompose(allVisualElements))
        .replace(List("content"), Extraction.decompose(contentWithExtractedEmbeds.map(_._1)))
        .replace(List("status"), Extraction.decompose(updatedStatus))
        .replace(List("notes"), Extraction.decompose(extractedArticle.notes :+ noteToAppend))

      compact(render(updatedArticle))
    } else { document }
  }

  /** Returns a tuple with (extracted embed, new content body) */
  def extractEmbedFromContent(content: String): (Option[String], String) = {
    val document = Jsoup.parseBodyFragment(content)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    val parsedContent = document.body()
    val firstEmbed = Option(parsedContent.select("embed").first())

    val willExtract = firstEmbed match {
      case Some(embed) =>
        val uuidStr = randomUUID().toString
        val textNode = new TextNode(uuidStr)
        embed.before(textNode)

        val shouldExtract = parsedContent.text().startsWith(uuidStr)
        textNode.remove()

        shouldExtract
      case None => false
    }

    if (willExtract) {
      val embedString = firstEmbed.map(e => {
        val html = e.outerHtml()
        e.remove()
        html
      })
      val ht = parsedContent.html()
      (embedString, ht)
    } else {
      (None, parsedContent.html())
    }
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  case class V16__Status(current: V16__ArticleStatus.Value, other: Set[V16__ArticleStatus.Value])
  case class V16__Content(content: String, language: String)
  case class V16__EditorNote(note: String, user: String, status: V16__Status, timestamp: Date)
  case class V16__VisualElement(resource: String, language: String)
  case class V16__Article(content: Seq[V16__Content],
                          visualElement: Seq[V16__VisualElement],
                          articleType: ArticleType.Value,
                          status: V16__Status,
                          notes: Seq[V16__EditorNote])

  object V16__ArticleStatus extends Enumeration {

    val IMPORTED, DRAFT, PUBLISHED, PROPOSAL, QUEUED_FOR_PUBLISHING, USER_TEST, AWAITING_QUALITY_ASSURANCE,
    QUALITY_ASSURED, AWAITING_UNPUBLISHING, UNPUBLISHED, ARCHIVED = Value
  }

}
