/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.model.FeideUserWrapper
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.tapir.*
import sttp.tapir.EndpointInput.AuthType

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success}

trait FeideAuth(using myNdlaApiClient: MyNDLAProvider) {
  private val headerName       = "FeideAuthorization"
  private val schemeName       = "FeideAuth"
  private val issuer           = "https://auth.dataporten.no"
  private val authorizationUrl = s"$issuer/oauth/authorization"
  private val tokenUrl         = s"$issuer/oauth/token"
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

  private def encodeFeideUserWrapper(user: FeideUserWrapper): String            = user.token
  private def decodeFeideUserWrapper(s: String): DecodeResult[FeideUserWrapper] = {
    myNdlaApiClient.getDomainUser(s) match {
      case Success(user) => DecodeResult.Value(FeideUserWrapper(s, Some(user)))
      case Failure(ex)   => DecodeResult.Error(s, ex)
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
}
