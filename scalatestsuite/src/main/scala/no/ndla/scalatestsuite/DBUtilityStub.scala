/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import no.ndla.database.DBUtility
import org.scalatestplus.mockito.MockitoSugar
import scalikejdbc.DBSession

import scala.util.Try

case class DBUtilityStub() extends DBUtility, MockitoSugar {
  private val session = mock[DBSession]

  override def rollbackOnFailure[T](f: DBSession => Try[T]): Try[T] = f(session)
  override def withSession[T](f: DBSession => T): T                 = f(session)
  override def tryWithSession[T](f: DBSession => Try[T]): Try[T]    = f(session)
  override def readOnly[T](f: DBSession => T): T                    = f(session)
  override def tryReadOnly[T](f: DBSession => Try[T]): Try[T]       = f(session)
}
