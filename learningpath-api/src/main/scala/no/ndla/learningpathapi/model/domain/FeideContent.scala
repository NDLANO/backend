/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.errors.AccessDeniedException
import scala.util.{Failure, Success, Try}

trait FeideContent {
  val feideId: FeideID

  def isOwner(feideId: FeideID): Try[FeideContent] = {
    if (this.feideId == feideId) Success(this)
    else Failure(AccessDeniedException("You do not have access to this entity."))
  }
}
