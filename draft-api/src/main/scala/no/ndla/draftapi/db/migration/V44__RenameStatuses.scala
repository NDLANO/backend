/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V44__RenameStatuses extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

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

  def convertStatus(oldStatus: String): String = {
    oldStatus match {
      case "DRAFT"                         => "PLANNED"
      case "PROPOSAL"                      => "IN_PROGRESS"
      case "USER_TEST"                     => "EXTERNAL_REVIEW"
      case "AWAITING_QUALITY_ASSURANCE"    => "QUALITY_ASSURANCE"
      case "QUEUED_FOR_LANGUAGE"           => "LANGUAGE"
      case "TRANSLATED"                    => "FOR_APPROVAL"
      case "QUEUED_FOR_PUBLISHING"         => "END_CONTROL"
      case "QUALITY_ASSURED"               => "END_CONTROL"
      case "QUALITY_ASSURED_DELAYED"       => "END_CONTROL"
      case "QUEUED_FOR_PUBLISHING_DELAYED" => "PUBLISH_DELAYED"
      case "AWAITING_UNPUBLISHING"         => "PUBLISHED"
      case "AWAITING_ARCHIVING"            => "PUBLISHED"
      case x                               => x
    }
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("status", status: JObject) =>
        val oldStatus = status.extract[V44__Status]
        val newStatus =
          oldStatus.copy(current = convertStatus(oldStatus.current), other = oldStatus.other.map(convertStatus))
        ("status", Extraction.decompose(newStatus))
      case x => x
    }
    compact(render(newArticle))
  }
}

case class V44__Status(current: String, other: Seq[String])
