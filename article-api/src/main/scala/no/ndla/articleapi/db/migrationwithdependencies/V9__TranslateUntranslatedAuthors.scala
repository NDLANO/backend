/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.ArticleApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.{FieldSerializer, Formats}
import org.json4s.FieldSerializer.ignore
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V9__TranslateUntranslatedAuthors(props: ArticleApiProperties) extends BaseJavaMigration {

  implicit val formats: Formats = org.json4s.DefaultFormats + FieldSerializer[V7_Article](
    ignore("id") orElse ignore("revision")
  ) ++ JavaTimeSerializers.all

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }: Unit

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

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL".map(rs => rs.long("count")).single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, Int, String)] = {
    sql"select id, revision, document from contentdata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.int("revision"), rs.string("document"))
      })
      .list()
  }

  def toNewAuthorType(author: V8_Author): V8_Author = {
    import props._
    val creatorMap      = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
    val processorMap    = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
    val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

    (
      creatorMap(author.`type`.toLowerCase),
      processorMap(author.`type`.toLowerCase),
      rightsholderMap(author.`type`.toLowerCase)
    ) match {
      case (t: String, _, _) => V8_Author(t.capitalize, author.name)
      case (_, t: String, _) => V8_Author(t.capitalize, author.name)
      case (_, _, t: String) => V8_Author(t.capitalize, author.name)
      case (_, _, _)         => V8_Author(author.`type`, author.name)
    }
  }

  def convertArticleUpdate(id: Long, revision: Int, document: String): V7_Article = {
    val articlev8 = read[V7_Article](document)

    val creators      = articlev8.copyright.creators.map(toNewAuthorType)
    val processors    = articlev8.copyright.processors.map(toNewAuthorType)
    val rightsholders = articlev8.copyright.rightsholders.map(toNewAuthorType)
    articlev8.copy(
      id = Some(id),
      revision = Some(revision),
      copyright = articlev8.copyright.copy(creators = creators, processors = processors, rightsholders = rightsholders)
    )
  }

  def updateArticle(articleMeta: V7_Article)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(articleMeta))

    sql"update contentdata set document = ${dataObject} where id = ${articleMeta.id}".update()
  }

}
