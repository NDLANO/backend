/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.model.domain.Language
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.{FieldSerializer, Formats}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.util.Date

class V6__AddLanguageToAll extends BaseJavaMigration {

  implicit val formats: Formats = org.json4s.DefaultFormats + FieldSerializer[V6_Article](
    ignore("id") orElse ignore("revision"))

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
      migrateConcepts
    }
  }

  //
  // Articles
  //

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map(convertArticleUpdate).foreach(updateArticle)
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL".map(rs => rs.long("count")).single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[V6_Article] = {
    sql"select id, revision, document from contentdata where document is not NULL order by id limit 1000 offset $offset"
      .map(rs => {
        val meta = read[V6_Article](rs.string("document"))
        meta.copy(id = Some(rs.long("id")), revision = Some(rs.int("revision")))

      })
      .list()
  }

  def convertArticleUpdate(articleMeta: V6_Article): V6_Article = {
    articleMeta.copy(
      title =
        articleMeta.title.map(t => V6_ArticleTitle(t.title, Some(Language.languageOrUnknown(t.language).toString))),
      content = articleMeta.content.map(c =>
        V6_ArticleContent(c.content, c.footNotes, Some(Language.languageOrUnknown(c.language).toString))),
      tags = articleMeta.tags.map(t => V6_ArticleTag(t.tags, Some(Language.languageOrUnknown(t.language).toString))),
      visualElement = articleMeta.visualElement.map(v =>
        V6_VisualElement(v.resource, Some(Language.languageOrUnknown(v.language).toString))),
      introduction = articleMeta.introduction.map(i =>
        V6_ArticleIntroduction(i.introduction, Some(Language.languageOrUnknown(i.language).toString))),
      metaDescription = articleMeta.metaDescription.map(m =>
        V6_ArticleMetaDescription(m.content, Some(Language.languageOrUnknown(m.language).toString)))
    )
  }

  def updateArticle(articleMeta: V6_Article)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(articleMeta))

    sql"update contentdata set document = $dataObject where id = ${articleMeta.id}".update()
  }

  //
  // Concepts
  //

  def migrateConcepts(implicit session: DBSession): Unit = {
    val count = countAllConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allConcepts(offset * 1000).map(convertConceptUpdate).foreach(updateConcept)
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllConcepts(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from conceptdata".map(rs => rs.long("count")).single()
  }

  def allConcepts(offset: Long)(implicit session: DBSession): Seq[V6_Concept] = {
    sql"select id, document from conceptdata order by id limit 1000 offset $offset"
      .map(rs => {
        val meta = read[V6_Concept](rs.string("document"))
        meta.copy(id = Some(rs.long("id")))

      })
      .list()
  }

  def convertConceptUpdate(concept: V6_Concept): V6_Concept = {
    concept.copy(
      title = concept.title.map(t => V6_ConceptTitle(t.title, Some(Language.languageOrUnknown(t.language).toString))),
      content =
        concept.content.map(c => V6_ConceptContent(c.content, Some(Language.languageOrUnknown(c.language).toString)))
    )
  }

  def updateConcept(conceptMeta: V6_Concept)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(conceptMeta))

    sql"update conceptdata set document = $dataObject where id = ${conceptMeta.id}".update()
  }

}

case class V6_ArticleTitle(title: String, language: Option[String])
case class V6_ArticleContent(content: String, footNotes: Option[Map[String, V6_FootNoteItem]], language: Option[String])
case class V6_ArticleTag(tags: Seq[String], language: Option[String])
case class V6_VisualElement(resource: String, language: Option[String])
case class V6_ArticleIntroduction(introduction: String, language: Option[String])
case class V6_ArticleMetaDescription(content: String, language: Option[String])
case class V6_RequiredLibrary(mediaType: String, name: String, url: String)
case class V6_Copyright(license: String, origin: String, authors: Seq[V6_Author])
case class V6_Author(`type`: String, name: String)
case class V6_FootNoteItem(title: String,
                           `type`: String,
                           year: String,
                           edition: String,
                           publisher: String,
                           authors: Seq[String])

case class V6_Article(id: Option[Long],
                      revision: Option[Int],
                      title: Seq[V6_ArticleTitle],
                      content: Seq[V6_ArticleContent],
                      copyright: V6_Copyright,
                      tags: Seq[V6_ArticleTag],
                      requiredLibraries: Seq[V6_RequiredLibrary],
                      visualElement: Seq[V6_VisualElement],
                      introduction: Seq[V6_ArticleIntroduction],
                      metaDescription: Seq[V6_ArticleMetaDescription],
                      metaImageId: Option[String],
                      created: Date,
                      updated: Date,
                      updatedBy: String,
                      articleType: String)

case class V6_Concept(id: Option[Long],
                      title: Seq[V6_ConceptTitle],
                      content: Seq[V6_ConceptContent],
                      authors: Seq[V6_Author],
                      created: Date,
                      updated: Date)
case class V6_ConceptTitle(title: String, language: Option[String])
case class V6_ConceptContent(content: String, language: Option[String])
