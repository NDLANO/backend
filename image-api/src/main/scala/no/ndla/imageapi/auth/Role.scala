/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.auth

import no.ndla.common.errors.AccessDeniedException
import no.ndla.imageapi.Props
import no.ndla.network.AuthUser

trait Role {
  this: Props =>
  val authRole: AuthRole

  class AuthRole {
    def userHasWriteRole(): Boolean = AuthUser.hasRole(props.RoleWithWriteAccess)

    def assertHasRole(role: String): Unit = {
      if (!AuthUser.hasRole(role))
        throw AccessDeniedException("User is missing required role to perform this operation")
    }
  }

}
