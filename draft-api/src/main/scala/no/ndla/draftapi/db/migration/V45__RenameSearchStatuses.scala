/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JArray
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V45__RenameSearchStatuses extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateUserData
    }

  def migrateUserData(implicit session: DBSession): Unit = {
    val count        = countAllUsers.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allUsers(offset * 1000).foreach { case (id, document) =>
        updateUser(convertUser(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllUsers(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from userdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allUsers(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from userdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateUser(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update userdata set document = $dataObject where id = $id"
      .update()
  }

  def convertSearch(search: String): String = {
    val path      = search.split("\\?")
    val params    = path.tail.mkString("").split("&")
    val converted = params.map(p => p.split("=").map(convertStatus).mkString("=")).mkString("&")
    s"${path.head}?$converted"
  }

  def convertStatus(status: String): String = {
    status match {
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

  private[migration] def convertUser(document: String): String = {
    val oldUser = parse(document)

    val newArticle = oldUser.mapField {
      case ("savedSearches", status: JArray) =>
        val oldSearches = status.extract[List[String]]
        val newStatus   = oldSearches.map(convertSearch);
        ("savedSearches", Extraction.decompose(newStatus))
      case x => x
    }
    compact(render(newArticle))
  }
}
