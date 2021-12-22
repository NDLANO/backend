/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration
import no.ndla.draftapi.model.domain.{ArticleMetaDescription, ArticleStatus, ArticleType}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Extraction.decompose
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{Formats, JArray, JValue}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class R__RemoveDummyMetaDescription extends BaseJavaMigration {
  implicit val formats
    : Formats = org.json4s.DefaultFormats + new EnumNameSerializer(ArticleStatus) + new EnumNameSerializer(ArticleType)

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

  def convertMetaDescription(metaDescription: List[ArticleMetaDescription]): JValue = {
    val newMetaDescriptions = metaDescription.map(meta => {
      meta.content match {
        case "Beskrivelse mangler" => ArticleMetaDescription("", meta.language)
        case _                     => ArticleMetaDescription(meta.content, meta.language)
      }
    })
    decompose(newMetaDescriptions)
  }

  def convertArticle(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("metaDescription", metaDescription: JArray) =>
        "metaDescription" -> convertMetaDescription(metaDescription.extract[List[ArticleMetaDescription]])
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
