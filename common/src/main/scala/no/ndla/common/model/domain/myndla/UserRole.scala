/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.myndla

import enumeratum.*
import enumeratum.{CirceEnum, EnumEntry}
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class UserRole(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}
object UserRole extends Enum[UserRole] with CirceEnum[UserRole] {
  case object EMPLOYEE extends UserRole("employee")
  case object STUDENT  extends UserRole("student")

  val values: IndexedSeq[UserRole] = findValues

  def all: Seq[String]                     = UserRole.values.map(_.entryName)
  def valueOf(s: String): Option[UserRole] = UserRole.withNameOption(s)
  implicit val schema: Schema[UserRole]    = schemaForEnumEntry[UserRole]
}
