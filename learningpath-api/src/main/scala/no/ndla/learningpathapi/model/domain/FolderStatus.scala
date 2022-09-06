/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import scala.util.{Failure, Success, Try}

object FolderStatus extends Enumeration {
  val PRIVATE: FolderStatus.Value = Value("private")
  val SHARED: FolderStatus.Value  = Value("shared")

  def all: Seq[String] = FolderStatus.values.map(_.toString).toSeq
  def valueOf(s: String): Option[FolderStatus.Value] = {
    FolderStatus.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[FolderStatus.Value] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

  def valueOfOrError(s: String): Try[FolderStatus.Value] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid folder status. Valid options are ${all.mkString(", ")}."))
      case Some(folderStatus) => Success(folderStatus)
    }
  }
}
