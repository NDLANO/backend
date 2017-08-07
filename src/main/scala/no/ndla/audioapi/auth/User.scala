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

    def id(): String = {
      if (AuthUser.get.isEmpty || AuthUser.get.get.isEmpty) {
        throw new AccessDeniedException(("User id required to perform this operation"))
      }

      AuthUser.get.get
    }

  }

}
