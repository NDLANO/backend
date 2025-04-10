/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.RollbackException
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

trait DBUtility {
  val DBUtil: DBUtility
  class DBUtility extends StrictLogging {
    def rollbackOnFailure[T](func: DBSession => Try[T]): Try[T] = {
      try {
        DB.localTx { session =>
          func(session) match {
            case Failure(ex)    => throw RollbackException(ex)
            case Success(value) => Success(value)
          }
        }
      } catch {
        case rbex: RollbackException =>
          logger.error("Rolling back transaction due to failure", rbex)
          Failure(rbex.ex)
      }
    }

    def withSession[T](func: DBSession => T): T = {
      DB.localTx { session =>
        func(session)
      }
    }

    /** Builds a where clause from a list of conditions. If the list is empty, an empty SQLSyntax object with no where
      * clause is returned.
      *
      * @param conditions
      *   A list of conditions to be joined with AND.
      * @return
      *   A SQLSyntax object representing the where clause.
      */
    def buildWhereClause(conditions: Seq[SQLSyntax]): SQLSyntax = if (conditions.nonEmpty) {
      val cc = conditions.foldLeft((true, sqls"where ")) { case (acc, cur) =>
        (
          false,
          if (acc._1) sqls"${acc._2} $cur" else sqls"${acc._2} and $cur"
        )
      }
      cc._2
    } else sqls""

  }
}
