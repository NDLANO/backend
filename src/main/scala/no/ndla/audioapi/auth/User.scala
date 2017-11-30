/*
 * Part of NDLA audio_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.auth

import no.ndla.audioapi.model.api.AccessDeniedException
import no.ndla.network.AuthUser


trait User {

  val authUser: AuthUser

  class AuthUser {
    def assertHasId(): Unit = userOrClientid()

    def userOrClientid(): String = {
      if (AuthUser.get.isDefined) {
        AuthUser.get.get
      }
      else if (AuthUser.getClientId.isDefined) {
        AuthUser.getClientId.get
      }
      else throw new AccessDeniedException("User id or Client id required to perform this operation")
    }

  }

}
