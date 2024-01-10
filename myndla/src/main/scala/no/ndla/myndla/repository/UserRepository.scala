/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.NotFoundException
import no.ndla.myndla.model.domain.{DBMyNDLAUser, MyNDLAUser, MyNDLAUserDocument, NDLASQLException}
import no.ndla.network.model.FeideID
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc.interpolation.SQLSyntax
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, scalikejdbcSQLInterpolationImplicitDef}

import scala.util.{Failure, Success, Try}

trait UserRepository {
  val userRepository: UserRepository

  class UserRepository extends StrictLogging {

    def getUsersPaginated(offset: Long, limit: Long)(implicit session: DBSession) = Try {
      val u = DBMyNDLAUser.syntax("u")
      sql"""
           select ${u.result.*}
           from ${DBMyNDLAUser.as(u)}
           order by ${u.id} asc
           limit $limit
           offset $offset
           """
        .map(DBMyNDLAUser.fromResultSet(u))
        .list()
    }

    def countUsers(implicit session: DBSession): Try[Long] = Try {
      sql"""select count(*) from ${DBMyNDLAUser.table}"""
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0)
    }

    implicit val formats: Formats = DBMyNDLAUser.repositorySerializer

    def getSession(readOnly: Boolean): DBSession =
      if (readOnly) ReadOnlyAutoSession
      else AutoSession

    def insertUser(feideId: FeideID, document: MyNDLAUserDocument)(implicit
        session: DBSession = AutoSession
    ): Try[MyNDLAUser] =
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(write(document))

        val userId = sql"""
        insert into ${DBMyNDLAUser.table} (feide_id, document)
        values ($feideId, $dataObject)
        """.updateAndReturnGeneratedKey()

        logger.info(s"Inserted new user with id: $userId")
        document.toFullUser(
          id = userId,
          feideId = feideId
        )
      }

    def updateUser(feideId: FeideID, user: MyNDLAUser)(implicit
        session: DBSession = AutoSession
    ): Try[MyNDLAUser] =
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(write(user))

        sql"""
        update ${DBMyNDLAUser.table}
                  set document=$dataObject
                  where feide_id=$feideId
        """.update()
      } match {
        case Failure(ex) => Failure(ex)
        case Success(count) if count == 1 =>
          logger.info(s"Updated user with feide_id $feideId")
          Success(user)
        case Success(count) =>
          Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
      }

    def userWithUsername(username: String)(implicit session: DBSession = ReadOnlyAutoSession) = userWhere(
      sqls"u.document->>'username'=$username"
    )

    def deleteUser(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[FeideID] = {
      Try(sql"delete from ${DBMyNDLAUser.table} where feide_id = $feideId".update()) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) if numRows != 1 =>
          Failure(NotFoundException(s"User with feide_id $feideId does not exist"))
        case Success(_) =>
          logger.info(s"Deleted user with feide_id $feideId")
          Success(feideId)
      }
    }

    def deleteAllUsers(implicit session: DBSession): Try[Unit] = Try {
      sql"delete from ${DBMyNDLAUser.table}".execute(): Unit
    }

    def resetSequences(implicit session: DBSession): Try[Unit] = Try {
      sql"alter sequence my_ndla_users_id_seq restart with 1".execute(): Unit
    }

    def userWithFeideId(feideId: FeideID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[MyNDLAUser]] =
      userWhere(sqls"u.feide_id=$feideId")

    def userWithId(userId: Long)(implicit session: DBSession): Try[Option[MyNDLAUser]] = userWhere(sqls"u.id=$userId")

    def userWhere(whereClause: SQLSyntax)(implicit session: DBSession): Try[Option[MyNDLAUser]] = Try {
      val u = DBMyNDLAUser.syntax("u")
      sql"select ${u.result.*} from ${DBMyNDLAUser.as(u)} where $whereClause"
        .map(DBMyNDLAUser.fromResultSet(u))
        .single()
    }

    def numberOfUsers()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${DBMyNDLAUser.table}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfFavouritedSubjects()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(favoriteSubject) from (select jsonb_array_elements_text(document->'favoriteSubjects') from ${DBMyNDLAUser.table}) as favoriteSubject"
        .map(rs => rs.long("count"))
        .single()
    }

    def getAllUsers(implicit session: DBSession): List[MyNDLAUser] = {
      val u = DBMyNDLAUser.syntax("u")
      sql"select ${u.result.*} from ${DBMyNDLAUser.as(u)}"
        .map(DBMyNDLAUser.fromResultSet(u))
        .list()
    }

  }
}
