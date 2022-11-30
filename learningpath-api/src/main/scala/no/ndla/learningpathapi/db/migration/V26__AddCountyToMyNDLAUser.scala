/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.learningpathapi.model.domain.UserRole
import no.ndla.network.model.FeideID
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.time.LocalDateTime

class V26__AddCountyToMyNDLAUser extends BaseJavaMigration {

  case class OldMyNDLAUser(favoriteSubjects: Seq[String], userRole: UserRole.Value, lastUpdated: LocalDateTime)
  case class NewMyNDLAUser(
      favoriteSubjects: Seq[String],
      userRole: UserRole.Value,
      lastUpdated: LocalDateTime,
      county: String
  )
  val repositorySerializer: Formats =
    DefaultFormats + new EnumNameSerializer(UserRole) ++ JavaTimeSerializers.all + FieldSerializer[OldMyNDLAUser](
      ignore("id") orElse
        ignore("feideId")
    )

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateCounty
    }
  }

  def migrateCounty(implicit session: DBSession): Unit = {
    allFeideIds.foreach(feideId => {
      val user = getMyNDLAUser(feideId)
      val updatedUser = NewMyNDLAUser(
        favoriteSubjects = user.favoriteSubjects,
        userRole = user.userRole,
        // We set lastUpdated to outdated in order to guarantee re-fetch of new information about county
        lastUpdated = LocalDateTime.now().minusDays(2),
        county = "temp"
      )
      updateMyNDLAUser(feideId, updatedUser)
    })
  }

  def updateMyNDLAUser(feideId: FeideID, document: NewMyNDLAUser)(implicit session: DBSession) = {
    implicit val formats: Formats = repositorySerializer

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(document))

    sql"update my_ndla_users set document=$dataObject where feide_id = $feideId".update.apply()
  }

  def getMyNDLAUser(feideId: FeideID)(implicit session: DBSession): OldMyNDLAUser = {
    implicit val formats: Formats = repositorySerializer

    sql"select document from my_ndla_users where feide_id = $feideId"
      .map(rs => read[OldMyNDLAUser](rs.string("document")))
      .single()
      .get
  }

  def allFeideIds()(implicit session: DBSession): Seq[String] = {
    sql"select feide_id from my_ndla_users"
      .map(rs => rs.string("feide_id"))
      .list()
  }
}
