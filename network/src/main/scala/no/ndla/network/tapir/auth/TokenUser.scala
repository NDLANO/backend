/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir.auth

import cats.implicits._
import no.ndla.network.jwt.JWTExtractor
import no.ndla.network.model.{JWTClaims, NdlaHttpRequest}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir._

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

case class TokenUser(
    id: String,
    permissions: Set[Permission],
    jwt: JWTClaims
) {
  def hasPermission(permission: Permission): Boolean             = permissions.contains(permission)
  def hasPermissions(permissions: Iterable[Permission]): Boolean = permissions.forall(hasPermission)
}

object TokenUser {

  /** Constructor to simplify creating testdata */
  def apply(id: String, scopes: Set[Permission]) = {
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
      )
    )

  }

  val PublicUser: TokenUser = TokenUser("public", Set.empty)
  val SystemUser: TokenUser = TokenUser("system", Permission.values.toSet)

  case class UserInfoException() extends RuntimeException("Could not build `TokenUser` from token.")

  private def fromExtractor(jWTExtractor: JWTExtractor) = {
    val userId   = jWTExtractor.extractUserId()
    val roles    = jWTExtractor.extractUserRoles()
    val userName = jWTExtractor.extractUserName()
    val clientId = jWTExtractor.extractClientId()

    userId.orElse(clientId).orElse(userName) match {
      case Some(userInfoName) => Success(TokenUser(userInfoName, Permission.fromStrings(roles)))
      case None               => Failure(UserInfoException())
    }
  }

  def fromToken(token: String): Try[TokenUser] = {
    val jWTExtractor = JWTExtractor(token)
    fromExtractor(jWTExtractor)
  }

  /* Only for scalatra, function can be removed when we move to tapir everywhere :^) */
  def fromScalatraRequest(request: HttpServletRequest): Try[TokenUser] = {
    val extractor = JWTExtractor(NdlaHttpRequest(request))
    fromExtractor(extractor)
  }

  implicit class MaybeTokenUser(self: Option[TokenUser]) {
    def hasPermission(permission: Permission): Boolean = self.exists(user => user.hasPermission(permission))
  }

  def encode(user: TokenUser): String = user.id
  def decode(s: String): DecodeResult[TokenUser] = fromToken(s) match {
    case Failure(ex)    => DecodeResult.Error(s, ex)
    case Success(value) => DecodeResult.Value(value)
  }
  implicit val userinfoCodec: Codec[String, TokenUser, TextPlain] = Codec.string.mapDecode(decode)(encode)
}
