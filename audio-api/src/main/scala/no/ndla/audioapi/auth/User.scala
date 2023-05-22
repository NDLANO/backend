/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.auth

import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.AuthUser

trait User {

  val authUser: AuthUser

  class AuthUser {
    def assertHasId(): Unit = userOrClientid()

    def userOrClientid(): String = {
      if (AuthUser.get.isDefined) {
        AuthUser.get.get
      } else if (AuthUser.getClientId.isDefined) {
        AuthUser.getClientId.get
      } else throw AccessDeniedException("User id or Client id required to perform this operation", unauthorized = true)
    }

  }

}