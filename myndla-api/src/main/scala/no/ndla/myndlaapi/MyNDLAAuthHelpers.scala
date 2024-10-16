/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.myndlaapi.model.domain.{ArenaGroup, MyNDLAUser}
import no.ndla.myndlaapi.service.UserService
import no.ndla.network.model.FeideAccessToken
import no.ndla.network.tapir.{AllErrors, TapirErrorHandling}
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.monad.MonadError
import sttp.tapir.*
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.EndpointInput.{AuthInfo, AuthType}
import sttp.tapir.server.PartialServerEndpoint

import scala.collection.immutable.ListMap

trait MyNDLAAuthHelpers {
  this: UserService with TapirErrorHandling =>

  object MyNDLAAuthHelpers {
    private val authScheme                           = AuthenticationScheme.Bearer.name
    private def filterHeaders(headers: List[String]) = headers.filter(_.toLowerCase.startsWith(authScheme.toLowerCase))
    private def stringPrefixWithSpace                = Mapping.stringPrefixCaseInsensitiveForList(authScheme + " ")
    val feideTokenAuthCodec: Codec[List[String], Option[FeideAccessToken], TextPlain] = {
      val codec = implicitly[Codec[List[String], Option[FeideAccessToken], CodecFormat.TextPlain]]
      Codec
        .id[List[String], CodecFormat.TextPlain](codec.format, Schema.binary)
        .map(filterHeaders(_))(identity)
        .map(stringPrefixWithSpace)
        .mapDecode(codec.decode)(codec.encode)
        .schema(codec.schema)
    }

    def feideOauth() = {
      val authType: AuthType.ScopedOAuth2 = EndpointInput.AuthType
        .OAuth2(None, None, ListMap.empty, None)
        .requiredScopes(Seq.empty)

      EndpointInput.Auth(
        input = sttp.tapir.header("FeideAuthorization")(feideTokenAuthCodec),
        challenge = WWWAuthenticateChallenge.bearer,
        authType = authType,
        info = AuthInfo.Empty.securitySchemeName("oauth2")
      )
    }

    implicit class authlessEndpointFeideExtension[A, I, E, O, R](self: Endpoint[Unit, I, AllErrors, O, R]) {
      type MaybeFeideToken            = Option[FeideAccessToken]
      type PartialFeideEndpoint[F[_]] = PartialServerEndpoint[MaybeFeideToken, MyNDLAUser, I, AllErrors, O, R, F]
      def requireMyNDLAUser[F[_]](
          requireArena: Boolean = false,
          requireArenaAdmin: Boolean = false
      ): PartialFeideEndpoint[F] = {
        val newEndpoint = self.securityIn(feideOauth())
        val authFunc: Option[FeideAccessToken] => Either[AllErrors, MyNDLAUser] = { maybeToken =>
          if (requireArenaAdmin) {
            userService.getArenaEnabledUser(maybeToken).handleErrorsOrOk match {
              case Right(user) if user.arenaGroups.contains(ArenaGroup.ADMIN) => Right(user)
              case Right(_)                                                   => Left(ErrorHelpers.forbidden)
              case Left(err)                                                  => Left(err)
            }
          } else if (requireArena) userService.getArenaEnabledUser(maybeToken).handleErrorsOrOk
          else userService.getMyNdlaUserDataDomain(maybeToken, List.empty).handleErrorsOrOk
        }
        val securityLogic = (m: MonadError[F]) => (a: Option[FeideAccessToken]) => m.unit(authFunc(a))
        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }

  }
}
