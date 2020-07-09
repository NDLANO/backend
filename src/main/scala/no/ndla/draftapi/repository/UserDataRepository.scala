/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.integration.DataSource
import no.ndla.draftapi.model.domain.UserData
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc.interpolation.SQLSyntax
import scalikejdbc._

import scala.util.{Success, Try}

trait UserDataRepository {
  this: DataSource =>
  val userDataRepository: UserDataRepository

  class UserDataRepository extends LazyLogging {
    implicit val formats: Formats = org.json4s.DefaultFormats + UserData.JSonSerializer

    def insert(userData: UserData)(implicit session: DBSession = AutoSession): UserData = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(userData))

      val userDataId: Long =
        sql"""
        insert into ${UserData.table} (user_id, document) values (${userData.userId}, $dataObject)
        """.updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted new user data: $userDataId")
      userData.copy(id = Some(userDataId))
    }

    def update(userData: UserData)(implicit  session: DBSession = AutoSession): Try[UserData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(userData))

      sql"""
          update ${UserData.table}
          set document=$dataObject
          where user_id=${userData.userId}
      """.update
        .apply

      logger.info(s"Updated user data ${userData.userId}")
      Success(userData)
    }

    def withId(id: Long): Option[UserData] =
      agreementWhere(sqls"agr.id=${id.toInt}")

    def withUserId(userId: String): Option[UserData] =
      agreementWhere(sqls"agr.user_id=$userId")
  }

  private def agreementWhere(whereClause: SQLSyntax)(
    implicit session: DBSession = ReadOnlyAutoSession): Option[UserData] = {
    val ud = UserData.syntax("ud")
    sql"select ${ud.result.*} from ${UserData.as(ud)} where $whereClause"
      .map(UserData.fromResultSet(ud))
      .single
      .apply()
  }

}
