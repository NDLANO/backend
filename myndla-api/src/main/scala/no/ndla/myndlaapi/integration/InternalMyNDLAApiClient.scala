/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import no.ndla.myndlaapi.service.UserService
import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.model.*

import scala.util.{Failure, Success}

class InternalMyNDLAApiClient(using userService: UserService) extends MyNDLAProvider {
  override def getFeideUserWrapperFromIdToken(idToken: FeideIdToken): Either[AuthException, FeideUserWrapper] =
    userService.getFeideUserWrapperFromIdToken(idToken) match {
      case Success(Some(userWrapper)) => Right(userWrapper)
      case Success(None)              => Left(MissingFeideUserException())
      case Failure(ex)                => Left(GetFeideUserWrapperException(ex))
    }
}
