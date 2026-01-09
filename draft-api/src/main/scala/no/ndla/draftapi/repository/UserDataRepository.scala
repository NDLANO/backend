/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.database.implicits.*
import no.ndla.draftapi.model.domain.UserData
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.Try

class UserDataRepository extends StrictLogging {
  def insert(userData: UserData)(using DBSession): Try[UserData] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(userData))

    tsql"""
      insert into ${UserData.table} (user_id, document) values (${userData.userId}, $dataObject)
    """
      .updateAndReturnGeneratedKey()
      .map { userDataId =>
        logger.info(s"Inserted new user data: $userDataId")
        userData.copy(id = Some(userDataId))
      }
  }

  def update(userData: UserData)(using DBSession): Try[UserData] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(userData))

    tsql"""
      update ${UserData.table}
      set document=$dataObject
      where user_id=${userData.userId}
    """
      .update()
      .map(_ => {
        logger.info(s"Updated user data ${userData.userId}")
        userData
      })
  }

  def withId(id: Long)(using DBSession): Try[Option[UserData]] = userDataWhere(sqls"ud.id=${id.toInt}")

  def withUserId(userId: String)(using DBSession): Try[Option[UserData]] = userDataWhere(sqls"ud.user_id=$userId")

  private def userDataWhere(whereClause: SQLSyntax)(using DBSession): Try[Option[UserData]] = {
    val ud = UserData.syntax("ud")
    tsql"select ${ud.result.*} from ${UserData.as(ud)} where $whereClause".map(UserData.fromResultSet(ud)).runSingle()
  }

}
