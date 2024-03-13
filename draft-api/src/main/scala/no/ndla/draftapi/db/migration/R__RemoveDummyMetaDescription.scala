/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import enumeratum.Json4s
import no.ndla.common.model.domain.{ArticleType, Description}
import no.ndla.common.model.domain.draft.DraftStatus
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Extraction.decompose
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc._

class R__RemoveDummyMetaDescription extends BaseJavaMigration {
  implicit val formats: Formats =
    org.json4s.DefaultFormats + Json4s.serializer(DraftStatus) + Json4s.serializer(ArticleType)

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
      allArticles(offset * 1000).foreach { case (id, document) =>
        updateArticle(convertArticle(document), id)
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

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }
}
