/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

object FolderStatus extends Enumeration {
  val PRIVATE: FolderStatus.Value = Value("private")
  val PUBLIC: FolderStatus.Value  = Value("public")
  val SHARED: FolderStatus.Value  = Value("shared")

  def valueOf(s: String): Option[FolderStatus.Value] = {
    FolderStatus.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[FolderStatus.Value] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }
}
