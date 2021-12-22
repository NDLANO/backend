/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.model.domain.Article
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{Extraction, Formats}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V33__ConvertLanguageUnknown extends BaseJavaMigration {
  implicit val formats: Formats = Article.jsonEncoder

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)
    val extractedArticle = oldArticle.extract[Article]
    val title = extractedArticle.title.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val content = extractedArticle.content.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val introduction = extractedArticle.introduction.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val metaDescription = extractedArticle.metaDescription.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val metaImage = extractedArticle.metaImage.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val tags = extractedArticle.tags.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val visualElement = extractedArticle.visualElement.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })

    val updated = oldArticle
      .replace(List("tags"), Extraction.decompose(tags))
      .replace(List("title"), Extraction.decompose(title))
      .replace(List("content"), Extraction.decompose(content))
      .replace(List("introduction"), Extraction.decompose(introduction))
      .replace(List("metaDescription"), Extraction.decompose(metaDescription))
      .replace(List("metaImage"), Extraction.decompose(metaImage))
      .replace(List("visualElement"), Extraction.decompose(visualElement))

    compact(render(updated))
  }
}
