/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.model.FeideID

import scala.util.{Failure, Success, Try}

trait FeideContent {
  val feideId: FeideID

  def isOwner(feideId: FeideID): Try[FeideContent] = {
    if (this.feideId == feideId) Success(this)
    else Failure(AccessDeniedException("You do not have access to this entity."))
  }
}
