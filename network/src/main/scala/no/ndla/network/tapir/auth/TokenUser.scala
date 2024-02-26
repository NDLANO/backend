/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir.auth

import cats.implicits._
import no.ndla.network.jwt.JWTExtractor
import no.ndla.network.model.JWTClaims
import sttp.model.HeaderNames
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.EndpointInput.{AuthInfo, AuthType}
import sttp.tapir._

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

case class TokenUser(
    id: String,
    permissions: Set[Permission],
    jwt: JWTClaims,
    originalToken: Option[String]
) {
  def hasPermission(permission: Permission): Boolean             = permissions.contains(permission)
  def hasPermissions(permissions: Iterable[Permission]): Boolean = permissions.forall(hasPermission)
}

object TokenUser {

  /** Constructor to simplify creating testdata */
  def apply(id: String, scopes: Set[Permission], token: Option[String]) = {
    new TokenUser(
      id = id,
      permissions = scopes,
      jwt = new JWTClaims(
        iss = None,
        sub = id.some,
        aud = Set("ndla_system").some,
        azp = id.some,
        exp = None,
        iat = 0L.some,
        scope = scopes.map(_.entryName).toList,
        ndla_id = id.some,
        user_name = id.some,
        jti = None
      ),
      token
    )

  }

  val PublicUser: TokenUser = TokenUser("public", Set.empty, None)
  val SystemUser: TokenUser = TokenUser("system", Permission.values.toSet, None)

  case class UserInfoException() extends RuntimeException("Could not build `TokenUser` from token.")

  private def fromExtractor(jWTExtractor: JWTExtractor, token: String) = {
    val userId   = jWTExtractor.extractUserId()
    val roles    = jWTExtractor.extractUserRoles()
    val userName = jWTExtractor.extractUserName()
    val clientId = jWTExtractor.extractClientId()

    userId.orElse(clientId).orElse(userName) match {
      case Some(userInfoName) =>
        Success(
          TokenUser(userInfoName, Permission.fromStrings(roles), Some(token))
        )
      case None => Failure(UserInfoException())
    }
  }

  def fromToken(token: String): Try[TokenUser] = {
    val jWTExtractor = JWTExtractor(token)
    fromExtractor(jWTExtractor, token)
  }

  implicit class MaybeTokenUser(self: Option[TokenUser]) {
    def hasPermission(permission: Permission): Boolean = self.exists(user => user.hasPermission(permission))
  }

  def encode(user: TokenUser): String = user.id
  def decode(s: String): DecodeResult[TokenUser] = fromToken(s) match {
    case Failure(ex)    => DecodeResult.Error(s, ex)
    case Success(value) => DecodeResult.Value(value)
  }

  private implicit val userinfoCodec: Codec[String, TokenUser, TextPlain] = Codec.string.mapDecode(decode)(encode)
  private val authScheme                                                  = AuthenticationScheme.Bearer.name
  private val codec = implicitly[Codec[List[String], Option[TokenUser], CodecFormat.TextPlain]]
  private def filterHeaders(headers: List[String]) = headers.filter(_.toLowerCase.startsWith(authScheme.toLowerCase))
  private def stringPrefixWithSpace                = Mapping.stringPrefixCaseInsensitiveForList(authScheme + " ")
  private val authCodec: Codec[List[String], Option[TokenUser], TextPlain] = Codec
    .id[List[String], CodecFormat.TextPlain](codec.format, Schema.binary)
    .map(filterHeaders(_))(identity)
    .map(stringPrefixWithSpace)
    .mapDecode(codec.decode)(codec.encode)
    .schema(codec.schema)

  def oauth2Input(permissions: Seq[Permission]): EndpointInput.Auth[Option[TokenUser], AuthType.ScopedOAuth2] = {
    val authType: AuthType.ScopedOAuth2 = EndpointInput.AuthType
      .OAuth2(
        None,
        None,
        ListMap.from(permissions.map(p => p.entryName -> p.entryName)),
        None
      )
      .requiredScopes(permissions.map(_.entryName))

    EndpointInput.Auth(
      input = sttp.tapir.header(HeaderNames.Authorization)(authCodec),
      challenge = WWWAuthenticateChallenge.bearer,
      authType = authType,
      info = AuthInfo.Empty.securitySchemeName("oauth2")
    )
  }
}
