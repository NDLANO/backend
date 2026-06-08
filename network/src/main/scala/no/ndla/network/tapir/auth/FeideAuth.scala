/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import cats.implicits.*
import no.ndla.common.configuration.BaseProps
import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.jwt.{JwsKeySelectorFactory, JwtVerifier}
import no.ndla.network.model.{FeideIdToken, FeideUserWrapper}
import no.ndla.network.tapir.AllErrors
import no.ndla.network.tapir.TapirUtil.errorOutputVariantFor
import sttp.model.StatusCode
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.tapir.*
import sttp.tapir.EndpointInput.AuthType
import sttp.tapir.server.PartialServerEndpoint

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success}

case class FeideAuth()(using
    jwsKeySelectorFactory: JwsKeySelectorFactory,
    myNdlaProvider: MyNDLAProvider,
    props: BaseProps,
) {
  private val headerName  = "FeideAuthorization"
  private val schemeName  = "FeideAuth"
  private val challenge   = WWWAuthenticateChallenge.bearer
  private val jwtVerifier = JwtVerifier(jwsKeySelectorFactory, props.feideIssuer, props.feideClientId, Set.empty)

  private val bearerMapping: Mapping[String, String] =
    Mapping.stringPrefixCaseInsensitive(AuthenticationScheme.Bearer.name + " ")

  private val feideUserWrapperMapping               = Mapping.fromDecode(decodeFeideUserWrapper)(encodeFeideUserWrapper)
  private val bearerFeideUserWrapperMapping         = bearerMapping.map(feideUserWrapperMapping)
  private val optionalBearerFeideUserWrapperMapping = TapirAuthUtil.makeOptionalMapping(bearerFeideUserWrapperMapping)

  private val feideIdTokenMapping       = Mapping.fromDecode(decodeFeideIdToken)(encodeFeideIdToken)
  private val bearerFeideIdTokenMapping = bearerMapping.map(feideIdTokenMapping)

  private val requiredFeideUserWrapperHeaderInput = header[String](headerName).map(bearerFeideUserWrapperMapping)
  private val optionalFeideUserWrapperHeaderInput =
    header[Option[String]](headerName).map(optionalBearerFeideUserWrapperMapping)

  private val requiredFeideIdTokenHeaderInput = header[String](headerName).map(bearerFeideIdTokenMapping)

  val feideRequiredAuth: EndpointInput.Auth[FeideUserWrapper, AuthType.OAuth2] =
    oauth2EndpointInput(requiredFeideUserWrapperHeaderInput)
  val feideOptionalAuth: EndpointInput.Auth[Option[FeideUserWrapper], AuthType.OAuth2] =
    oauth2EndpointInput(optionalFeideUserWrapperHeaderInput)

  val feideIdTokenRequiredAuth: EndpointInput.Auth[FeideIdToken, AuthType.OAuth2] =
    oauth2EndpointInput(requiredFeideIdTokenHeaderInput)

  extension [INPUT, OUTPUT, R](self: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R]) {
    private def selfWithErrorOut: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R] = self
      .errorOutVariantPrepend(errorOutputVariantFor(StatusCode.Unauthorized.code))
      .errorOutVariantPrepend(errorOutputVariantFor(StatusCode.Forbidden.code))

    def withFeideUser[F[_]]: PartialServerEndpoint[FeideUserWrapper, FeideUserWrapper, INPUT, AllErrors, OUTPUT, R, F] =
      selfWithErrorOut.securityIn(feideRequiredAuth).serverSecurityLogicPure(_.asRight)

    def withOptionalFeideUser[F[_]]
        : PartialServerEndpoint[Option[FeideUserWrapper], Option[FeideUserWrapper], INPUT, AllErrors, OUTPUT, R, F] =
      selfWithErrorOut.securityIn(feideOptionalAuth).serverSecurityLogicPure(_.asRight)

    def withFeideIdToken[F[_]]: PartialServerEndpoint[FeideIdToken, FeideIdToken, INPUT, AllErrors, OUTPUT, R, F] =
      selfWithErrorOut.securityIn(feideIdTokenRequiredAuth).serverSecurityLogicPure(_.asRight)
  }

  private def oauth2EndpointInput[T](
      headerInput: EndpointIO.Header[T]
  ): EndpointInput.Auth[T, EndpointInput.AuthType.OAuth2] = EndpointInput.Auth(
    headerInput,
    challenge,
    EndpointInput.AuthType.OAuth2(Some(props.feideAuthorizationUrl), Some(props.feideTokenUrl), ListMap(), None),
    EndpointInput.AuthInfo.Empty.securitySchemeName(schemeName),
  )

  private def encodeFeideUserWrapper(user: FeideUserWrapper): String            = user.idToken.originalToken
  private def decodeFeideUserWrapper(s: String): DecodeResult[FeideUserWrapper] = decodeFeideIdToken(s).flatMap {
    idToken =>
      myNdlaProvider.getFeideUserWrapperFromIdToken(idToken) match {
        case Right(user) => DecodeResult.Value(user)
        case Left(ex)    => DecodeResult.Error(s, ex)
      }
  }

  private def encodeFeideIdToken(token: FeideIdToken): String           = token.originalToken
  private def decodeFeideIdToken(s: String): DecodeResult[FeideIdToken] = jwtVerifier.decode[FeideIdToken](s) match {
    case Success(token) => DecodeResult.Value(token)
    case Failure(ex)    => DecodeResult.Error(s, ex)
  }
}
