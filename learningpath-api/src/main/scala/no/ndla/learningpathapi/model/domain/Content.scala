/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import scala.util.{Failure, Success, Try}

trait FeideContent {
  val feideId: FeideID

  def isOwner(feideId: FeideID): Try[FeideContent] = {
    if (this.feideId == feideId) Success(this)
    else Failure(AccessDeniedException("You do not have access to this entity."))
  }
}

trait FolderContent extends FeideContent {
  def status: FolderStatus.Value
  def isFavorite: Boolean
  val feideId: FeideID

  def isPublic: Boolean = this.status == FolderStatus.PUBLIC
  def isShared: Boolean = this.status == FolderStatus.SHARED

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
