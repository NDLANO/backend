/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

object UserRole extends Enumeration {
  val EMPLOYEE: UserRole.Value = Value("employee")
  val STUDENT: UserRole.Value  = Value("student")

  def valueOf(s: String): Option[FolderStatus.Value]         = FolderStatus.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[FolderStatus.Value] = s.flatMap(valueOf)

}
