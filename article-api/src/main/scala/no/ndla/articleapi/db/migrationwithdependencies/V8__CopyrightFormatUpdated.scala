/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.db.migration.{V6_Article, V6_Author}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.{FieldSerializer, Formats}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.time.LocalDateTime

class V8__CopyrightFormatUpdated(props: ArticleApiProperties) extends BaseJavaMigration {
  import props._

  implicit val formats: Formats =
    org.json4s.DefaultFormats + FieldSerializer[V7_Article](ignore("id") orElse ignore("revision"))

  override def migrate(context: Context) = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }

  //
  // Articles
  //

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map(t => convertArticleUpdate(t._1, t._2, t._3)).foreach(updateArticle)
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession) = {
    sql"select count(*) from contentdata where document is not NULL".map(rs => rs.long("count")).single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, Int, String)] = {
    sql"select id, revision, document from contentdata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.int("revision"), rs.string("document"))
      })
      .list()
  }

  def toNewAuthorType(author: V6_Author): V8_Author = {
    val creatorMap      = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
    val processorMap    = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
    val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

    (
      creatorMap(author.`type`.toLowerCase),
      processorMap(author.`type`.toLowerCase),
      rightsholderMap(author.`type`.toLowerCase)
    ) match {
      case (t: String, None, None) => V8_Author(t.capitalize, author.name)
      case (None, t: String, None) => V8_Author(t.capitalize, author.name)
      case (None, None, t: String) => V8_Author(t.capitalize, author.name)
      case (_, _, _)               => V8_Author(author.`type`, author.name)
    }
  }

  def convertArticleUpdate(id: Long, revision: Int, document: String): V7_Article = {
    val articlev6 = read[V6_Article](document)
    val articlev8 = read[V7_Article](document)

    // If entry contains V7 features -> Don't update.
    if (
      articlev8.copyright.creators.nonEmpty ||
      articlev8.copyright.processors.nonEmpty ||
      articlev8.copyright.rightsholders.nonEmpty ||
      articlev8.copyright.validFrom.nonEmpty ||
      articlev8.copyright.validTo.nonEmpty
    ) {

      articlev8.copy(id = None, revision = Some(revision))
    } else {
      val creators =
        articlev6.copyright.authors.filter(a => oldCreatorTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)
      // Filters out processor authors with old type `redaksjonelt` during import process since `redaksjonelt` exists both in processors and creators.
      val processors = articlev6.copyright.authors
        .filter(a => oldProcessorTypes.contains(a.`type`.toLowerCase))
        .filterNot(a => a.`type`.toLowerCase == "redaksjonelt")
        .map(toNewAuthorType)
      val rightsholders = articlev6.copyright.authors
        .filter(a => oldRightsholderTypes.contains(a.`type`.toLowerCase))
        .map(toNewAuthorType)
      articlev8.copy(
        id = Some(id),
        revision = Some(revision),
        copyright = V7_Copyright(
          articlev6.copyright.license,
          articlev6.copyright.origin,
          creators,
          processors,
          rightsholders,
          None,
          None,
          None
        )
      )
    }

  }

  def updateArticle(articleMeta: V7_Article)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(articleMeta))

    sql"update contentdata set document = ${dataObject} where id = ${articleMeta.id}".update()
  }

}

case class V8_ArticleTitle(title: String, language: Option[String])
case class V8_ArticleContent(content: String, footNotes: Option[Map[String, V8_FootNoteItem]], language: Option[String])
case class V8_ArticleTag(tags: Seq[String], language: Option[String])
case class V8_VisualElement(resource: String, language: Option[String])
case class V8_ArticleIntroduction(introduction: String, language: Option[String])
case class V8_ArticleMetaDescription(content: String, language: Option[String])
case class V8_RequiredLibrary(mediaType: String, name: String, url: String)
case class V8_Author(`type`: String, name: String)
case class V8_FootNoteItem(
    title: String,
    `type`: String,
    year: String,
    edition: String,
    publisher: String,
    authors: Seq[String]
)

case class V7_Copyright(
    license: String,
    origin: String,
    creators: Seq[V8_Author],
    processors: Seq[V8_Author],
    rightsholders: Seq[V8_Author],
    agreement: Option[Long],
    validFrom: Option[LocalDateTime],
    validTo: Option[LocalDateTime]
)
case class V7_Article(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[V8_ArticleTitle],
    content: Seq[V8_ArticleContent],
    copyright: V7_Copyright,
    tags: Seq[V8_ArticleTag],
    requiredLibraries: Seq[V8_RequiredLibrary],
    visualElement: Seq[V8_VisualElement],
    introduction: Seq[V8_ArticleIntroduction],
    metaDescription: Seq[V8_ArticleMetaDescription],
    metaImageId: Option[String],
    created: LocalDateTime,
    updated: LocalDateTime,
    updatedBy: String,
    articleType: String
)
