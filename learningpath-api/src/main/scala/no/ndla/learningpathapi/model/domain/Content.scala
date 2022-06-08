/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import java.util.UUID
import scala.util.{Failure, Success, Try}

trait Content {
  def id: Option[UUID]
  def feideId: FeideID

  def doIfIdExists[T](func: UUID => T): Try[T] = {
    this.id match {
      case None     => Failure(MissingIdException("Entity did not have id when expected. This is a bug."))
      case Some(id) => Success(func(id))
    }
  }

  def doFlatIfIdExists[T](func: UUID => Try[T]): Try[T] = {
    this.id match {
      case None     => Failure(MissingIdException("Entity did not have id when expected. This is a bug."))
      case Some(id) => func(id)
    }
  }

  def isOwner(feideId: FeideID): Try[Content] = {
    if (this.feideId == feideId) Success(this)
    else Failure(AccessDeniedException("You do not have access to this entity."))
  }

}

trait FolderContent extends Content {
  def status: FolderStatus.Value
  def isFavorite: Boolean

  def isPublic: Boolean = this.status == FolderStatus.PUBLIC

  def canDelete(feideId: FeideID): Try[_] = {
    isOwner(feideId) match {
      case Failure(exception)            => Failure(exception)
      case Success(_) if this.isFavorite => Failure(DeleteFavoriteException("Favorite folder can not be deleted"))
      case Success(x)                    => Success(x)
    }
  }

  def hasReadAccess(feideId: FeideID): Try[_] = {
    if (isPublic) {
      Success(this)
    } else {
      isOwner(feideId)
    }
  }

}
