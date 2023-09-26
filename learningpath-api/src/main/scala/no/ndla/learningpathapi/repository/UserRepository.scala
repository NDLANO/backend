/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain.{
  DBMyNDLAUser,
  MyNDLAUser,
  MyNDLAUserDocument,
  NDLASQLException,
  NotFoundException
}
import no.ndla.network.model.FeideID
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc.interpolation.SQLSyntax
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, scalikejdbcSQLInterpolationImplicitDef}

import scala.util.{Failure, Success, Try}

trait UserRepository {
  this: DataSource with DBMyNDLAUser =>
  val userRepository: UserRepository

  class UserRepository extends StrictLogging {

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

    def userWithFeideId(feideId: FeideID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[MyNDLAUser]] =
      userWhere(sqls"u.feide_id=$feideId")

    private def userWhere(whereClause: SQLSyntax)(implicit session: DBSession): Try[Option[MyNDLAUser]] = Try {
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

    def numberOfSubjects()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(favoriteSubject) from (select distinct jsonb_array_elements_text(document->'favoriteSubjects') from ${DBMyNDLAUser.table}) as favoriteSubject"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfFavouritedSubjects()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(favoriteSubject) from (select jsonb_array_elements_text(document->'favoriteSubjects') from ${DBMyNDLAUser.table}) as favoriteSubject"
        .map(rs => rs.long("count"))
        .single()
    }

  }
}
