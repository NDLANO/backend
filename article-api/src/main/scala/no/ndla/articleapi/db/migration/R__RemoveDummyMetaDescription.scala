/*
 * Part of NDLA article-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.common.model.domain.Description
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Extraction.decompose
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc.*

class R__RemoveDummyMetaDescription extends BaseJavaMigration {
  implicit val formats: Formats = org.json4s.DefaultFormats

  override def getChecksum: Integer = 1 // Change this to something else if you want to repeat migration

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
      allArticles(offset * 1000).map { case (id, document) =>
        updateArticle(convertArticle(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"""select count(*) from contentdata where document is not NULL"""
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"""
         select id, document from contentdata
         where document is not null
         order by id limit 1000 offset $offset
      """
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def convertMetaDescription(metaDescription: List[Description]): JValue = {
    val newMetaDescriptions = metaDescription.map(meta => {
      meta.content match {
        case "Beskrivelse mangler" => Description("", meta.language)
        case _                     => Description(meta.content, meta.language)
      }
    })
    decompose(newMetaDescriptions)
  }

  def convertArticle(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("metaDescription", metaDescription: JArray) =>
        "metaDescription" -> convertMetaDescription(metaDescription.extract[List[Description]])
      case x => x
    }
    compact(render(newArticle))
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = $dataObject where id = $id"
      .update()
  }
}
