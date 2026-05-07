/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import no.ndla.common.auth.Permission
import no.ndla.common.configuration.BaseProps
import no.ndla.network.jwt.JWTExtractor
import sttp.tapir.*
import sttp.tapir.EndpointInput.AuthType

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success}

case class UserInfoException() extends RuntimeException("Could not build `TokenUser` from token.")

trait NdlaAuth(using props: BaseProps) {
  private val schemeName               = "NDLAAuth"
  private val authorizationUrl         = s"${props.ndlaAuth0Issuer}/authorize"
  private val tokenUrl                 = s"${props.ndlaAuth0Issuer}/oauth/token"
  private val tokenUserMapping         = Mapping.fromDecode(decodeTokenUser)(encodeTokenUser)
  private val optionalTokenUserMapping = TapirAuthUtil.makeOptionalMapping(tokenUserMapping)

  val ndlaOptionalAuth: EndpointInput.Auth[Option[TokenUser], AuthType.OAuth2] = TapirAuth
    .oauth2
    .authorizationCodeFlowOptional(authorizationUrl, tokenUrl)
    .securitySchemeName(schemeName)
    .map(optionalTokenUserMapping)

  def ndlaOptionalAuth(
      requiredPermissions: Seq[Permission]
  ): EndpointInput.Auth[Option[TokenUser], AuthType.ScopedOAuth2] = {
    val scopes         = ListMap.from(props.ndlaAuth0Scopes.map(p => p.entryName -> p.entryName))
    val requiredScopes = requiredPermissions.map(_.entryName)
    TapirAuth
      .oauth2
      .authorizationCodeFlowOptional(authorizationUrl, tokenUrl, scopes = scopes)
      .securitySchemeName(schemeName)
      .map(optionalTokenUserMapping)
      .requiredScopes(requiredScopes)
  }

  private def encodeTokenUser(user: TokenUser): String            = user.id
  private def decodeTokenUser(s: String): DecodeResult[TokenUser] = {
    val jWTExtractor = JWTExtractor(s)
    fromExtractor(jWTExtractor, s) match {
      case Failure(ex)    => DecodeResult.Error(s, ex)
      case Success(value) => DecodeResult.Value(value)
    }
  }

  private def fromExtractor(jWTExtractor: JWTExtractor, token: String) = {
    val userId   = jWTExtractor.extractUserId()
    val roles    = jWTExtractor.extractUserRoles()
    val userName = jWTExtractor.extractUserName()
    val clientId = jWTExtractor.extractClientId()

    userId.orElse(clientId).orElse(userName) match {
      case Some(userInfoName) => Success(TokenUser(userInfoName, Permission.fromStrings(roles), Some(token)))
      case None               => Failure(UserInfoException())
    }
  }
}
