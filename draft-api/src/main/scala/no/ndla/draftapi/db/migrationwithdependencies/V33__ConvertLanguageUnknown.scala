/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.{DraftApiProperties, Props}
import no.ndla.draftapi.model.domain.DBArticle
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc._

class V33__ConvertLanguageUnknown(properties: DraftApiProperties) extends BaseJavaMigration with DBArticle with Props {
  override val props: DraftApiProperties = properties
  implicit val formats: Formats          = Draft.jsonEncoder

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).foreach { case (id, document) =>
        updateArticle(convertArticleUpdate(document), id)
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
  case class V33_Title(title: String, language: String)
  case class V33_Content(content: String, language: String)
  case class V33_VisualElement(resource: String, language: String)
  case class V33_Introduction(introduction: String, language: String)
  case class V33_Description(content: String, language: String)
  case class V33_MetaImage(imageId: String, altText: String, language: String)
  case class V33_Tag(tags: Seq[String], language: String)
  case class V33_Draft(
      title: Seq[V33_Title],
      content: Seq[V33_Content],
      visualElement: Seq[V33_VisualElement],
      introduction: Seq[V33_Introduction],
      metaDescription: Seq[V33_Description],
      metaImage: Seq[V33_MetaImage],
      tags: Seq[V33_Tag]
  )

  private[migrationwithdependencies] def convertArticleUpdate(document: String): String = {
    val oldArticle       = parse(document)
    val extractedArticle = oldArticle.extract[V33_Draft]
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
