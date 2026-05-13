/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import no.ndla.common.auth.Permission
import no.ndla.network.model.ForbiddenException
import no.ndla.network.tapir.auth.TokenUser

trait JwtDecoder[A] {
  def decode(claims: JWTClaimsSetWrapper, token: String): Either[ForbiddenException, A]
}

object JwtDecoder {
  given JwtDecoder[TokenUser] = (claims, token) =>
    for {
      id             <- claims.stringClaim("https://ndla.no/ndla_id").orElse(claims.stringClaim("sub"))
      rawPermissions <- claims.stringListClaim("permissions")
      permissions     = Permission.fromStrings(rawPermissions)
    } yield TokenUser(id, permissions, Some(token))
}
