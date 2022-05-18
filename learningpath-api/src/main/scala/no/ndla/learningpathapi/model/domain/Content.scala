/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import scala.util.{Failure, Success, Try}

trait Content {
  def id: Option[Long]
  def feideId: Option[FeideID]

  def doIfIdExists[T](func: Long => T): Try[T] = {
    this.id match {
      case None     => Failure(MissingIdException("Entity did not have id when expected. This is a bug."))
      case Some(id) => Success(func(id))
    }
  }

  def doFlatIfIdExists[T](func: Long => Try[T]): Try[T] = {
    this.id match {
      case None     => Failure(MissingIdException("Entity did not have id when expected. This is a bug."))
      case Some(id) => func(id)
    }
  }

  def isOwner(feideId: FeideID): Try[Content] = {
    this.feideId match {
      case None => Failure(MissingIdException("Entity did not have feide_id when expected. This is a bug."))
      case Some(id) if id != feideId => Failure(AccessDeniedException("You do not have access to this entity."))
      case Some(_)                   => Success(this)
    }
  }

}

trait FolderContent extends Content {
  def status: FolderStatus.Value

  def isPublic: Boolean = this.status == FolderStatus.PUBLIC

  def hasReadAccess(feideId: FeideID): Try[_] = {
    if (isPublic) {
      Success(this)
    } else {
      isOwner(feideId)
    }
  }

}
