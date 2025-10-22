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
import no.ndla.draftapi.model.domain.UserData
import org.postgresql.util.PGobject
import scalikejdbc.*
import scalikejdbc.interpolation.SQLSyntax

import scala.util.{Success, Try}

class UserDataRepository extends StrictLogging {
  def insert(userData: UserData)(implicit session: DBSession = AutoSession): Try[UserData] = {
    Try {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(CirceUtil.toJsonString(userData))

      val userDataId: Long = sql"""
        insert into ${UserData.table} (user_id, document) values (${userData.userId}, $dataObject)
        """.updateAndReturnGeneratedKey()

      logger.info(s"Inserted new user data: $userDataId")
      userData.copy(id = Some(userDataId))
    }
  }

  def update(userData: UserData)(implicit session: DBSession = AutoSession): Try[UserData] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(userData))

    val _ = sql"""
          update ${UserData.table}
          set document=$dataObject
          where user_id=${userData.userId}
      """.update()

    logger.info(s"Updated user data ${userData.userId}")
    Success(userData)
  }

  def userDataCount(implicit session: DBSession = AutoSession): Long = {
    sql"select count(distinct user_id) from ${UserData.table} where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def withId(id: Long): Option[UserData] = userDataWhere(sqls"ud.id=${id.toInt}")

  def withUserId(userId: String): Option[UserData] = userDataWhere(sqls"ud.user_id=$userId")

  private def userDataWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Option[UserData] = {
    val ud = UserData.syntax("ud")
    sql"select ${ud.result.*} from ${UserData.as(ud)} where $whereClause".map(UserData.fromResultSet(ud)).single()
  }

}
