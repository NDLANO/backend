/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import cats.implicits.*
import no.ndla.common.auth.Permission
import no.ndla.common.configuration.BaseProps
import no.ndla.network.jwt.{JwsKeySelectorFactory, JwtVerifier}
import no.ndla.network.tapir.TapirUtil.errorOutputVariantFor
import no.ndla.network.tapir.{AllErrors, ErrorHelpers}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointInput.AuthType
import sttp.tapir.server.PartialServerEndpoint

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success}

case class NdlaAuth()(using
    jwsKeySelectorFactory: JwsKeySelectorFactory,
    errorHelpers: ErrorHelpers,
    props: BaseProps,
) {
  private val schemeName               = "NDLAAuth"
  private val authorizationUrl         = s"${props.ndlaAuth0Issuer}authorize"
  private val tokenUrl                 = s"${props.ndlaAuth0Issuer}oauth/token"
  private val tokenUserMapping         = Mapping.fromDecode(decodeTokenUser)(encodeTokenUser)
  private val optionalTokenUserMapping = TapirAuthUtil.makeOptionalMapping(tokenUserMapping)
  private val jwtVerifier              = JwtVerifier(
    jwsKeySelectorFactory,
    props.ndlaAuth0Issuer,
    Some(props.ndlaAuth0Audience),
    Set(props.ndlaAuth0LegacyIssuer),
  )

  val ndlaOptionalAuth: EndpointInput.Auth[Option[TokenUser], AuthType.OAuth2] = TapirAuth
    .oauth2
    .authorizationCodeFlowOptional(authorizationUrl, tokenUrl)
    .securitySchemeName(schemeName)
    .map(optionalTokenUserMapping)

  /** Creates a Tapir endpoint input with documentation (but not validation) for required scopes. */
  def ndlaRequiredAuthWithPermissions(
      requiredPermissions: Seq[Permission]
  ): EndpointInput.Auth[TokenUser, AuthType.ScopedOAuth2] = {
    val scopes         = ListMap.from(props.ndlaAuth0Scopes.map(p => p.entryName -> p.entryName))
    val requiredScopes = requiredPermissions.map(_.entryName)
    TapirAuth
      .oauth2
      .authorizationCodeFlow(authorizationUrl, tokenUrl, scopes = scopes)
      .securitySchemeName(schemeName)
      .map(tokenUserMapping)
      .requiredScopes(requiredScopes)
  }

  extension [INPUT, OUTPUT, R](self: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R]) {
    private def selfWithErrorOut: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R] = self
      .errorOutVariantPrepend(errorOutputVariantFor(StatusCode.Unauthorized.code))
      .errorOutVariantPrepend(errorOutputVariantFor(StatusCode.Forbidden.code))

    def requirePermission[F[_]](
        requiredPermission: Permission*
    ): PartialServerEndpoint[TokenUser, TokenUser, INPUT, AllErrors, OUTPUT, R, F] = selfWithErrorOut
      .securityIn(ndlaRequiredAuthWithPermissions(requiredPermission))
      .serverSecurityLogicPure(requireScope(requiredPermission*))

    def withOptionalUser[F[_]]
        : PartialServerEndpoint[Option[TokenUser], Option[TokenUser], INPUT, AllErrors, OUTPUT, R, F] = selfWithErrorOut
      .securityIn(ndlaOptionalAuth)
      .serverSecurityLogicPure(Right(_))
  }

  /** Helper function that returns function one can pass to `serverSecurityLogicPure` to require a specific scope for
    * some endpoint.
    */
  private def requireScope(scope: Permission*): TokenUser => Either[AllErrors, TokenUser] = {
    case user if user.hasPermissions(scope) => user.asRight
    case _                                  => errorHelpers.forbidden.asLeft
  }

  private def encodeTokenUser(user: TokenUser): String            = user.originalToken.getOrElse("")
  private def decodeTokenUser(s: String): DecodeResult[TokenUser] = {
    jwtVerifier.decode[TokenUser](s) match {
      case Success(user) => DecodeResult.Value(user)
      case Failure(ex)   => DecodeResult.Error(s, ex)
    }
  }
}
