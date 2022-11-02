/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

object UserRole extends Enumeration {
  val TEACHER: UserRole.Value = Value("teacher")
  val STUDENT: UserRole.Value = Value("student")

  def valueOf(s: String): Option[FolderStatus.Value]         = FolderStatus.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[FolderStatus.Value] = s.flatMap(valueOf)

}
