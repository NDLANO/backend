/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import no.ndla.common.auth.Permission
import no.ndla.network.model.{FeideIdToken, ForbiddenException}
import no.ndla.network.tapir.auth.TokenUser

trait JwtDecoder[A] {
  def decode(claims: JWTClaimsSetWrapper, token: String): Either[ForbiddenException, A]
}

object JwtDecoder {
  implicit lazy val tokenUser: JwtDecoder[TokenUser] = (claims, token) =>
    for {
      id             <- claims.stringClaim("https://ndla.no/ndla_id").orElse(claims.stringClaim("sub"))
      rawPermissions <- claims.stringListClaim("permissions")
      permissions     = Permission.fromStrings(rawPermissions)
    } yield TokenUser(id, permissions, Some(token))

  implicit lazy val feideIdToken: JwtDecoder[FeideIdToken] = (claims, token) =>
    for {
      iss                    <- claims.stringClaim("iss")
      jti                    <- claims.stringClaim("jti")
      aud                    <- claims.stringListClaim("aud")
      sub                    <- claims.stringClaim("sub")
      iat                    <- claims.issueTime
      exp                    <- claims.expirationTime
      email                  <- claims.stringClaim("email")
      name                   <- claims.stringClaim("name")
      userid_sec             <- claims.stringListClaim("https://n.feide.no/claims/userid_sec")
      eduPersonPrincipalName <- claims.stringClaim("https://n.feide.no/claims/eduPersonPrincipalName")
    } yield FeideIdToken(iss, jti, aud, sub, iat, exp, email, name, userid_sec, eduPersonPrincipalName, token)
}
