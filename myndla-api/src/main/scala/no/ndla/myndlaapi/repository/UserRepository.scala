/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.DBUtil.buildWhereClause
import no.ndla.common.errors.{NotFoundException, RollbackException}
import no.ndla.myndlaapi.model.domain.{MyNDLAUser, MyNDLAUserDocument, NDLASQLException, UserRole}
import no.ndla.network.model.FeideID
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

trait UserRepository {
  val userRepository: UserRepository

  class UserRepository extends StrictLogging {

    def getUsersPaginated(offset: Long, limit: Long, filterTeachers: Boolean, query: Option[String])(implicit
        session: DBSession
    ): Try[(Long, List[MyNDLAUser])] = Try {
      val u = MyNDLAUser.syntax("u")

      val teacherClause = Option.when(filterTeachers)(sqls"u.document->>'userRole' = ${UserRole.EMPLOYEE.toString}")
      val queryClause = query.map(q => {
        val qString = s"%$q%"
        sqls"u.document->>'displayName' ilike $qString or u.document->>'username' ilike $qString"
      })

      val whereClause = buildWhereClause((teacherClause ++ queryClause).toSeq)

      val count: Long = sql"""
              select count(*)
              from ${MyNDLAUser.as(u)}
              $whereClause
           """
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0)

      val users = sql"""
           select ${u.result.*}
           from ${MyNDLAUser.as(u)}
           $whereClause
           order by ${u.id} asc
           limit $limit
           offset $offset
           """
        .map(MyNDLAUser.fromResultSet(u))
        .list()

      count -> users
    }

    def getSession(readOnly: Boolean): DBSession =
      if (readOnly) ReadOnlyAutoSession
      else AutoSession

    def rollbackOnFailure[T](func: DBSession => Try[T]): Try[T] = {
      try {
        DB.localTx { session =>
          func(session) match {
            case Failure(ex)    => throw RollbackException(ex)
            case Success(value) => Success(value)
          }
        }
      } catch {
        case RollbackException(ex) => Failure(ex)
      }
    }

    def insertUser(feideId: FeideID, document: MyNDLAUserDocument)(implicit
        session: DBSession = AutoSession
    ): Try[MyNDLAUser] =
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(CirceUtil.toJsonString(document))

        val userId = sql"""
        update ${MyNDLAUser.table}
        set document=$dataObject
        where feide_id=$feideId
        """.updateAndReturnGeneratedKey()

        logger.info(s"Inserted new user with id: $userId")
        document.toFullUser(
          id = userId,
          feideId = feideId
        )
      }

    def updateUserById(userId: Long, user: MyNDLAUser)(implicit session: DBSession): Try[MyNDLAUser] = {
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(CirceUtil.toJsonString(user))

        sql"""
        update ${MyNDLAUser.table}
        set document=$dataObject
        where id=$userId
        """.update()
      } match {
        case Failure(ex) => Failure(ex)
        case Success(count) if count == 1 =>
          logger.info(s"Updated user with user_id $userId")
          Success(user)
        case Success(count) =>
          Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
      }
    }

    def updateUser(feideId: FeideID, user: MyNDLAUser)(implicit
        session: DBSession = AutoSession
    ): Try[MyNDLAUser] =
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(CirceUtil.toJsonString(user))

        sql"""
        update ${MyNDLAUser.table}
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

    def userWithUsername(username: String)(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[MyNDLAUser]] =
      userWhere(
        sqls"u.document->>'username'=$username"
      )

    def deleteUser(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[FeideID] = {
      Try(sql"delete from ${MyNDLAUser.table} where feide_id = $feideId".update()) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) if numRows != 1 =>
          Failure(NotFoundException(s"User with feide_id $feideId does not exist"))
        case Success(_) =>
          logger.info(s"Deleted user with feide_id $feideId")
          Success(feideId)
      }
    }

    def deleteAllUsers(implicit session: DBSession): Try[Unit] = Try {
      sql"delete from ${MyNDLAUser.table}".execute(): Unit
    }

    def resetSequences(implicit session: DBSession): Try[Unit] = Try {
      sql"alter sequence my_ndla_users_id_seq restart with 1".execute(): Unit
    }

    def userWithFeideId(feideId: FeideID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[MyNDLAUser]] =
      userWhere(sqls"u.feide_id=$feideId")

    def userWithId(userId: Long)(implicit session: DBSession): Try[Option[MyNDLAUser]] = userWhere(sqls"u.id=$userId")

    def userWhere(whereClause: SQLSyntax)(implicit session: DBSession): Try[Option[MyNDLAUser]] = Try {
      val u = MyNDLAUser.syntax("u")
      sql"select ${u.result.*} from ${MyNDLAUser.as(u)} where $whereClause"
        .map(MyNDLAUser.fromResultSet(u))
        .single()
    }

    /** Returns false if the user was inserted, true if the user already existed. */
    def reserveFeideIdIfNotExists(feideId: FeideID)(implicit session: DBSession): Try[Boolean] = {
      Try {
        sql"""
            with inserted as (
                insert into ${MyNDLAUser.table}
                (feide_id, document)
                values ($feideId, null)
                on conflict do nothing
                returning id, feide_id, document
            )
            select id, feide_id, document
            from inserted
         """
          .map(rs => rs.stringOpt("feide_id"))
          .single()
          .flatten
      }.map {
        case Some(_) => false
        case None    => true
      }
    }

    def numberOfUsers()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${MyNDLAUser.table}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfFavouritedSubjects()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(favoriteSubject) from (select jsonb_array_elements_text(document->'favoriteSubjects') from ${MyNDLAUser.table}) as favoriteSubject"
        .map(rs => rs.long("count"))
        .single()
    }

    def getAllUsers(implicit session: DBSession): List[MyNDLAUser] = {
      val u = MyNDLAUser.syntax("u")
      sql"select ${u.result.*} from ${MyNDLAUser.as(u)}"
        .map(MyNDLAUser.fromResultSet(u))
        .list()
    }

  }
}
