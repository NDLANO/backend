/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.model.domain.myndla.auth.AuthUtility
import no.ndla.common.model.domain.myndla.MyNDLAUser
import no.ndla.myndlaapi.service.UserService
import no.ndla.network.model.FeideAccessToken
import no.ndla.network.tapir.{AllErrors, TapirErrorHandling}
import sttp.monad.MonadError
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint

trait MyNDLAAuthHelpers {
  this: UserService & TapirErrorHandling =>

  object MyNDLAAuthHelpers {
    implicit class authlessEndpointFeideExtension[A, I, E, O, R](self: Endpoint[Unit, I, AllErrors, O, R]) {
      private type MaybeFeideToken = Option[FeideAccessToken]
      private type PartialFeideEndpoint[F[_]] =
        PartialServerEndpoint[MaybeFeideToken, MyNDLAUser, I, AllErrors, O, R, F]
      def requireMyNDLAUser[F[_]](
          requireArena: Boolean = false,
      ): PartialFeideEndpoint[F] = {
        val newEndpoint = self.securityIn(AuthUtility.feideOauth())
        val authFunc: Option[FeideAccessToken] => Either[AllErrors, MyNDLAUser] = { maybeToken =>
          if (requireArena) userService.getArenaEnabledUser(maybeToken).handleErrorsOrOk
          else userService.getMyNdlaUserDataDomain(maybeToken).handleErrorsOrOk
        }
        val securityLogic = (m: MonadError[F]) => (a: Option[FeideAccessToken]) => m.unit(authFunc(a))
        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }

  }
}
