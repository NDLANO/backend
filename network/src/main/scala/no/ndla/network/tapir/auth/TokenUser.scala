/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import cats.implicits.*
import no.ndla.network.model.{CombinedUserWithTokenUser, JWTClaims}

case class TokenUser(id: String, permissions: Set[Permission], jwt: JWTClaims, originalToken: Option[String]) {
  def hasPermission(permission: Permission): Boolean             = permissions.contains(permission)
  def hasPermissions(permissions: Iterable[Permission]): Boolean = permissions.forall(hasPermission)
  def toCombined: CombinedUserWithTokenUser                      = CombinedUserWithTokenUser(this, None)
}

object TokenUser {

  /** Constructor to simplify creating testdata */
  def apply(id: String, scopes: Set[Permission], token: Option[String]): TokenUser = {
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
        jti = None,
      ),
      token,
    )

  }

  val PublicUser: TokenUser = TokenUser("public", Set.empty, None)
  val SystemUser: TokenUser = TokenUser("system", Permission.values.toSet, None)

  extension (self: Option[TokenUser]) {
    def hasPermission(permission: Permission): Boolean = self.exists(user => user.hasPermission(permission))
  }
}
