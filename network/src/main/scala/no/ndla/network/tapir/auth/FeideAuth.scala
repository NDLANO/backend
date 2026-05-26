/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import cats.implicits.*
import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.model.FeideUserWrapper
import no.ndla.network.tapir.{AllErrors, ErrorHelpers}
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.tapir.*
import sttp.tapir.EndpointInput.AuthType
import sttp.tapir.server.PartialServerEndpoint

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success}

case class FeideAuth()(using myNdlaApiClient: MyNDLAProvider, errorHelpers: ErrorHelpers) {
  private val headerName       = "FeideAuthorization"
  private val schemeName       = "FeideAuth"
  private val issuerUrl        = "https://auth.dataporten.no"
  private val authorizationUrl = s"$issuerUrl/oauth/authorization"
  private val tokenUrl         = s"$issuerUrl/oauth/token"
  private val challenge        = WWWAuthenticateChallenge.bearer

  private val bearerMapping: Mapping[String, String] =
    Mapping.stringPrefixCaseInsensitive(AuthenticationScheme.Bearer.name + " ")
  private val feideUserWrapperMapping               = Mapping.fromDecode(decodeFeideUserWrapper)(encodeFeideUserWrapper)
  private val bearerFeideUserWrapperMapping         = bearerMapping.map(feideUserWrapperMapping)
  private val optionalBearerFeideUserWrapperMapping = TapirAuthUtil.makeOptionalMapping(bearerFeideUserWrapperMapping)
  private val optionalBearerMapping                 = TapirAuthUtil.makeOptionalMapping(bearerMapping)

  private val requiredHeaderInput          = header[String](headerName).map(bearerFeideUserWrapperMapping)
  private val optionalHeaderInput          = header[Option[String]](headerName).map(optionalBearerFeideUserWrapperMapping)
  private val optionalUncheckedHeaderInput = header[Option[String]](headerName).map(optionalBearerMapping)

  val feideRequiredAuth: EndpointInput.Auth[FeideUserWrapper, AuthType.OAuth2] =
    oauth2EndpointInput(requiredHeaderInput)
  val feideOptionalAuth: EndpointInput.Auth[Option[FeideUserWrapper], AuthType.OAuth2] =
    oauth2EndpointInput(optionalHeaderInput)
  val feideOptionalUncheckedAuth: EndpointInput.Auth[Option[String], AuthType.OAuth2] =
    oauth2EndpointInput(optionalUncheckedHeaderInput)

  extension [INPUT, OUTPUT, R](self: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R]) {
    def withFeideUser[F[_]]: PartialServerEndpoint[FeideUserWrapper, FeideUserWrapper, INPUT, AllErrors, OUTPUT, R, F] =
      self
        .securityIn(feideRequiredAuth)
        .serverSecurityLogicPure {
          case user @ FeideUserWrapper(_, Some(_)) => user.asRight
          case _                                   => errorHelpers.unauthorized.asLeft
        }

    def withOptionalFeideUser[F[_]]
        : PartialServerEndpoint[Option[FeideUserWrapper], Option[FeideUserWrapper], INPUT, AllErrors, OUTPUT, R, F] =
      self
        .securityIn(feideOptionalAuth)
        .serverSecurityLogicPure {
          case user @ Some(FeideUserWrapper(_, Some(_))) => user.asRight
          case _                                         => None.asRight
        }
  }

  private def oauth2EndpointInput[T](
      headerInput: EndpointIO.Header[T]
  ): EndpointInput.Auth[T, EndpointInput.AuthType.OAuth2] = EndpointInput.Auth(
    headerInput,
    challenge,
    EndpointInput.AuthType.OAuth2(Some(authorizationUrl), Some(tokenUrl), ListMap(), None),
    EndpointInput.AuthInfo.Empty.securitySchemeName(schemeName),
  )

  private def encodeFeideUserWrapper(user: FeideUserWrapper): String            = user.token
  private def decodeFeideUserWrapper(s: String): DecodeResult[FeideUserWrapper] = {
    myNdlaApiClient.getDomainUser(s) match {
      case Success(user) => DecodeResult.Value(FeideUserWrapper(s, Some(user)))
      case Failure(ex)   => DecodeResult.Error(s, ex)
    }
  }
}
