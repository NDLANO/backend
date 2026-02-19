/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.NotFoundException
import no.ndla.database.DBUtility
import no.ndla.database.implicits.*
import no.ndla.myndlaapi.uuidParameterFactory
import no.ndla.myndlaapi.model.domain.{DBRobotDefinition, RobotDefinition}
import no.ndla.network.model.FeideID
import scalikejdbc.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

class RobotRepository(using dbUtility: DBUtility, dbRobotDefinition: DBRobotDefinition) extends StrictLogging {
  def getSession(readOnly: Boolean): DBSession =
    if (readOnly) dbUtility.readOnlySession
    else dbUtility.autoSession

  def withTx[T](func: DBSession => T): T = dbUtility.localTx(func)

  def updateRobotDefinition(robot: RobotDefinition)(implicit session: DBSession): Try[Unit] = Try {
    val column = dbRobotDefinition.column.c

    val _ = withSQL {
      update(dbRobotDefinition)
        .set(
          column("status")        -> robot.status.entryName,
          column("updated")       -> robot.updated,
          column("shared")        -> robot.shared,
          column("configuration") -> dbUtility.asJsonb(robot.configuration),
        )
        .where
        .eq(column("id"), robot.id)
    }.update()

    logger.info(s"Updted robot definition with ID: ${robot.id}")
  }

  def insertRobotDefinition(robot: RobotDefinition)(session: DBSession): Try[RobotDefinition] = Try {
    val column = dbRobotDefinition.column.c

    withSQL {
      insert
        .into(dbRobotDefinition)
        .namedValues(
          column("id")            -> robot.id,
          column("feide_id")      -> robot.feideId,
          column("status")        -> robot.status.entryName,
          column("created")       -> robot.created,
          column("updated")       -> robot.updated,
          column("shared")        -> robot.shared,
          column("configuration") -> dbUtility.asJsonb(robot.configuration),
        )
    }.update()(using session): Unit

    logger.info(s"Inserted new robot definition with ID: ${robot.id}")

    robot
  }

  def getRobotsWithFeideId(feideId: FeideID)(implicit session: DBSession): Try[List[RobotDefinition]] = Try {
    val r = dbRobotDefinition.syntax("r")
    tsql"""
           select ${r.result.*}
           from ${dbRobotDefinition.as(r)}
           where feide_id = $feideId
           order by ${r.updated} desc
         """.map(RobotDefinition.fromResultSet(r)).runListFlat()
  }.flatten

  def getRobotWithId(robotId: UUID)(implicit session: DBSession): Try[Option[RobotDefinition]] = {
    val r = dbRobotDefinition.syntax("r")
    tsql"""
           select ${r.result.*}
           from ${dbRobotDefinition.as(r)}
           where id = $robotId
         """.map(RobotDefinition.fromResultSet(r)).runSingleFlat()
  }

  def deleteRobotDefinition(robotId: UUID)(implicit session: DBSession): Try[Unit] = {
    val result = Try {
      withSQL {
        delete.from(dbRobotDefinition).where.eq(dbRobotDefinition.column.c("id"), robotId)
      }.update()
    }

    result match {
      case Failure(ex)                      => Failure(ex)
      case Success(numRows) if numRows != 1 => Failure(NotFoundException(s"Robot with id $robotId does not exist"))
      case Success(_)                       => Success(())
    }
  }
}
