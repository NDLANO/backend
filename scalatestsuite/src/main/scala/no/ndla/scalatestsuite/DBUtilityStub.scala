/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import no.ndla.database.{DBUtility, ReadableDbSession, WriteableDbSession}
import org.scalatestplus.mockito.MockitoSugar
import scalikejdbc.DBSession

import scala.util.Try

case class DBUtilityStub() extends DBUtility, MockitoSugar {
  private val session      = mock[DBSession]
  private val writeSession = session.asInstanceOf[WriteableDbSession]
  private val readSession  = session.asInstanceOf[ReadableDbSession]

  override def rollbackOnFailure[T](f: WriteableDbSession => Try[T]): Try[T] = f(writeSession)
  override def withSession[T](f: WriteableDbSession => T): T                 = f(writeSession)
  override def tryWithSession[T](f: WriteableDbSession => Try[T]): Try[T]    = f(writeSession)
  override def readOnly[T](f: ReadableDbSession => T): T                     = f(readSession)
  override def tryReadOnly[T](f: ReadableDbSession => Try[T]): Try[T]        = f(readSession)
}
