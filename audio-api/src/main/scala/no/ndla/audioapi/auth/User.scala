/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.auth

import cats.effect.IO
import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.model.RequestInfo

trait User {

  val authUser: AuthUser

  class AuthUser {
    private val accessDeniedException =
      AccessDeniedException("User id or Client id required to perform this operation", unauthorized = true)

    def userOrClientId(): IO[String] = {
      RequestInfo.get.flatMap(info => {
        val userId   = info.authUser.userId
        val clientId = info.authUser.clientId
        (userId, clientId) match {
          case (Some(user), _)     => IO.pure(user)
          case (_, Some(clientId)) => IO.pure(clientId)
          case _                   => IO.raiseError(accessDeniedException)
        }
      })
    }

  }

}
