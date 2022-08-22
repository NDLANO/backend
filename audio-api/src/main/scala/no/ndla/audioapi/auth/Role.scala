/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.auth

import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.AuthUser

trait Role {

  val authRole: AuthRole

  class AuthRole {

    def assertHasRole(role: String): Unit = {
      if (!AuthUser.hasRole(role))
        throw AccessDeniedException("User is missing required role to perform this operation")
    }
  }

}
