/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, parser}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, *}

class V58__MigrateSavedSearchUrls extends BaseJavaMigration {

  private def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from userdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allRows(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, user_id from userdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def updateRow(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update userdata set document = $dataObject where id = $id"
      .update()
  }

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session => migrateRows(using session) }

  private def migrateRows(implicit session: DBSession): Unit = {
    val count        = countAllRows.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allRows(offset * 1000).map { case (id, document) =>
        updateRow(convertDocument(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  def convertDocument(document: String): String = {
    val oldUserData = parser.parse(document).flatMap(_.as[V58_UserData]).toTry.get

    val newUserData = V58_UserData(
      userId = oldUserData.userId,
      savedSearches = oldUserData.savedSearches.map(el =>
        el.map { value =>
          V58_SavedSearch(
            value.searchUrl.replace("/concept", "/content").replace("status=", "draft-status="),
            value.searchPhrase
          )
        }
      ),
      latestEditedArticles = oldUserData.latestEditedArticles,
      latestEditedConcepts = oldUserData.latestEditedConcepts,
      favoriteSubjects = oldUserData.favoriteSubjects
    )
    newUserData.asJson.noSpaces
  }
}

case class V58_SavedSearch(searchUrl: String, searchPhrase: String)
object V58_SavedSearch {
  implicit def encoder: Encoder[V58_SavedSearch] = deriveEncoder
  implicit def decoder: Decoder[V58_SavedSearch] = deriveDecoder
}
case class V58_UserData(
    userId: String,
    savedSearches: Option[Seq[V58_SavedSearch]],
    latestEditedArticles: Option[Seq[String]],
    latestEditedConcepts: Option[Seq[String]],
    favoriteSubjects: Option[Seq[String]]
)
object V58_UserData {
  implicit val decoder: Decoder[V58_UserData] = deriveDecoder
  implicit val encoder: Encoder[V58_UserData] = deriveEncoder
}
