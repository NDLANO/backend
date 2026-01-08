/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder
import no.ndla.common.CirceUtil
import no.ndla.common.TryUtil.throwIfInterrupted
import no.ndla.common.errors.RollbackException
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

class DBUtility extends StrictLogging {
  def rollbackOnFailure[T](func: WriteableDbSession => Try[T]): Try[T] = {
    try {
      DB.localTx { session =>
        val writeableSession = WriteableDbSession(session)
        func(writeableSession) match {
          case Failure(ex)    => throw RollbackException(ex)
          case Success(value) => Success(value)
        }
      }
    } catch {
      case rbex: RollbackException =>
        logger.info("Rolling back transaction due to failure", rbex)
        Failure(rbex.ex)
    }
  }

  def writeSession[T](func: WriteableDbSession => T): T = writeSession(s => Success(func(s))).get

  def writeSession[T](func: WriteableDbSession => Try[T]): Try[T] = Try
    .throwIfInterrupted {
      DB.localTx { session =>
        val writeableSession = WriteableDbSession(session)
        func(writeableSession)
      }
    }
    .flatten

  def readOnly[T](func: ReadableDbSession => T): T = readOnly(s => Success(func(s))).get

  def readOnly[T](func: ReadableDbSession => Try[T]): Try[T] = Try
    .throwIfInterrupted {
      DB.readOnly { session =>
        val readableSession = ReadableDbSession(session)
        func(readableSession)
      }
    }
    .flatten

  /** Builds a where clause from a list of conditions. If the list is empty, an empty SQLSyntax object with no where
    * clause is returned.
    *
    * @param conditions
    *   A list of conditions to be joined with AND.
    * @return
    *   A SQLSyntax object representing the where clause.
    */
  def buildWhereClause(conditions: Seq[SQLSyntax]): SQLSyntax =
    if (conditions.nonEmpty) {
      val cc = conditions.foldLeft((true, sqls"where ")) { case (acc, cur) =>
        (
          false,
          if (acc._1) sqls"${acc._2} $cur"
          else sqls"${acc._2} and $cur",
        )
      }
      cc._2
    } else sqls""

  def asRawJsonb(value: String): ParameterBinderWithValue = {
    val obj = new PGobject()
    obj.setType("jsonb")
    obj.setValue(value)
    ParameterBinder(obj, (ps, idx) => ps.setObject(idx, obj))
  }

  def asJsonb[T: Encoder](value: T): ParameterBinderWithValue = {
    val serialized = CirceUtil.toJsonString(value)
    asRawJsonb(serialized)
  }
}
