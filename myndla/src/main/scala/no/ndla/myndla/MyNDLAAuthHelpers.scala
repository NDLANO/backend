/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla

import no.ndla.myndla.model.domain.MyNDLAUser
import no.ndla.myndla.service.UserService
import no.ndla.network.tapir.{AllErrors, TapirErrorHelpers}
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.monad.MonadError
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.EndpointInput.{AuthInfo, AuthType}
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir._

import scala.collection.immutable.ListMap

trait MyNDLAAuthHelpers {
  this: UserService with TapirErrorHelpers =>

  object MyNDLAAuthHelpers {
    private val authScheme                           = AuthenticationScheme.Bearer.name
    private def filterHeaders(headers: List[String]) = headers.filter(_.toLowerCase.startsWith(authScheme.toLowerCase))
    private def stringPrefixWithSpace                = Mapping.stringPrefixCaseInsensitiveForList(authScheme + " ")
    private val feideTokenAuthCodec: Codec[List[String], Option[FeideAccessToken], TextPlain] = {
      val codec = implicitly[Codec[List[String], Option[FeideAccessToken], CodecFormat.TextPlain]]
      Codec
        .id[List[String], CodecFormat.TextPlain](codec.format, Schema.binary)
        .map(filterHeaders(_))(identity)
        .map(stringPrefixWithSpace)
        .mapDecode(codec.decode)(codec.encode)
        .schema(codec.schema)
    }

    private def feideOauth() = {
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
              case Right(user) if user.arenaAdmin.contains(true) => Right(user)
              case Right(_)                                      => Left(ErrorHelpers.forbidden)
              case Left(err)                                     => Left(err)
            }
          } else if (requireArena) userService.getArenaEnabledUser(maybeToken).handleErrorsOrOk
          else userService.getMyNdlaUserDataDomain(maybeToken).handleErrorsOrOk
        }
        val securityLogic = (m: MonadError[F]) => (a: Option[FeideAccessToken]) => m.unit(authFunc(a))
        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }

  }
}
