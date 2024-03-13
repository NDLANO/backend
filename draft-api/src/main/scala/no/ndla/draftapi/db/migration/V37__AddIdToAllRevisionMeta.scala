/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import enumeratum.Json4s
import no.ndla.common.model.domain.draft.RevisionStatus
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.ext.{JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.time.LocalDateTime
import java.util.UUID

class V37__AddIdToAllRevisionMeta extends BaseJavaMigration {
  implicit val formats: Formats =
    org.json4s.DefaultFormats ++ JavaTimeSerializers.all ++ JavaTypesSerializers.all + Json4s.serializer(RevisionStatus)

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

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("revisionMeta", revisionMeta) =>
        val oldMetas = revisionMeta.extract[Seq[V36__RevisionMeta]]
        val newMetas = oldMetas.map(old => V37__RevisionMeta(UUID.randomUUID(), old.revisionDate, old.note, old.status))
        ("revisionMeta", Extraction.decompose(newMetas))
      case x => x
    }
    compact(render(newArticle))
  }
}

case class V36__RevisionMeta(revisionDate: LocalDateTime, note: String, status: RevisionStatus)
case class V37__RevisionMeta(id: UUID, revisionDate: LocalDateTime, note: String, status: RevisionStatus)
