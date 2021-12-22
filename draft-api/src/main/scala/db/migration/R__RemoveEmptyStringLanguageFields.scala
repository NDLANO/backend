/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.model.domain
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Extraction.decompose
import org.json4s.jackson.Serialization
import org.json4s.native.JsonMethods.{compact, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class R__RemoveEmptyStringLanguageFields extends BaseJavaMigration {
  override def getChecksum: Integer = 1 // Change this to something else if you want to repeat migration

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
        case (id, document) => updateArticle(convertArticle(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"""select count(*) from articledata where document is not NULL"""
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"""
         select id, document from articledata
         where document is not null
         order by id limit 1000 offset $offset
      """
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def convertArticle(document: String): String = {
    implicit val formats = domain.Article.jsonEncoder

    val oldArticle = Serialization.read[domain.Article](document)
    val newArticle = oldArticle.copy(
      content = oldArticle.content.filterNot(_.isEmpty),
      introduction = oldArticle.introduction.filterNot(_.isEmpty),
      metaDescription = oldArticle.metaDescription.filterNot(_.isEmpty),
      metaImage = oldArticle.metaImage.filterNot(_.isEmpty),
      tags = oldArticle.tags.filterNot(_.isEmpty),
      title = oldArticle.title.filterNot(_.isEmpty),
      visualElement = oldArticle.visualElement.filterNot(_.isEmpty),
    )
    compact(render(decompose(newArticle)))
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }
}
