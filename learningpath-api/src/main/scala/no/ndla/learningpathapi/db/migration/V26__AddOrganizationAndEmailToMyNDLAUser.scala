/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.UserRole
import no.ndla.network.model.FeideID
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

import java.time.LocalDateTime

class V26__AddOrganizationAndEmailToMyNDLAUser extends BaseJavaMigration {

  case class OldMyNDLAUser(favoriteSubjects: Seq[String], userRole: UserRole.Value, lastUpdated: LocalDateTime)
  case class NewMyNDLAUser(
      favoriteSubjects: Seq[String],
      userRole: UserRole.Value,
      lastUpdated: LocalDateTime,
      organization: String,
      email: String
  )
  implicit val formats: Formats =
    DefaultFormats +
      new EnumNameSerializer(UserRole) ++
      JavaTimeSerializers.all +
      NDLADate.Json4sSerializer +
      FieldSerializer[OldMyNDLAUser](
        ignore("id") orElse
          ignore("feideId")
      )

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateOrganization
    }

  def migrateOrganization(implicit session: DBSession): Unit = {
    allFeideIds.foreach(feideId => {
      val twoDaysAgo = LocalDateTime.now().minusDays(2)
      val maybeUser  = getMyNDLAUser(feideId)
      maybeUser match {
        case Some(user) =>
          val updatedUser = NewMyNDLAUser(
            favoriteSubjects = user.favoriteSubjects,
            userRole = user.userRole,
            // We set lastUpdated to outdated in order to guarantee re-fetch of organization and email information
            lastUpdated = twoDaysAgo,
            organization = "temp",
            email = "example@email.com"
          )
          updateMyNDLAUser(feideId, updatedUser): Unit
        case None =>
          val newUser = NewMyNDLAUser(
            favoriteSubjects = Seq.empty,
            userRole = UserRole.STUDENT,
            // We set lastUpdated to outdated in order to guarantee re-fetch of organization, email and userRole information
            lastUpdated = twoDaysAgo,
            organization = "temp",
            email = "example@email.com"
          )
          createMyNDLAUser(feideId, newUser): Unit
      }
    })
  }

  def updateMyNDLAUser(feideId: FeideID, document: NewMyNDLAUser)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(document))

    sql"update my_ndla_users set document=$dataObject where feide_id = $feideId".update.apply()
  }

  def createMyNDLAUser(feideId: FeideID, document: NewMyNDLAUser)(implicit session: DBSession = AutoSession): Long = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(document))

    sql"""
    insert into my_ndla_users (feide_id, document)
    values ($feideId, $dataObject)
    """.updateAndReturnGeneratedKey()
  }

  def getMyNDLAUser(feideId: FeideID)(implicit session: DBSession): Option[OldMyNDLAUser] = {
    sql"select document from my_ndla_users where feide_id = $feideId"
      .map(rs => read[OldMyNDLAUser](rs.string("document")))
      .single()
  }

  def allFeideIds(implicit session: DBSession): Seq[String] = {
    val folderFeideIds     = sql"select feide_id from folders".map(rs => rs.string("feide_id")).list()
    val myNDLAUserFeideIds = sql"select feide_id from my_ndla_users".map(rs => rs.string("feide_id")).list()
    (folderFeideIds ++ myNDLAUserFeideIds).distinct
  }
}
