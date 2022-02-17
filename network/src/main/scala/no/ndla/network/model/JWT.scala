/*
 * Part of NDLA network.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import org.json4s.native.JsonMethods.parse
import pdi.jwt.JwtClaim

case class JWTClaims(
    iss: Option[String],
    sub: Option[String],
    aud: Option[Set[String]],
    azp: Option[String],
    exp: Option[Long],
    iat: Option[Long],
    scope: List[String],
    ndla_id: Option[String],
    user_name: Option[String],
    jti: Option[String]
)

object JWTClaims {
  implicit val formats = org.json4s.DefaultFormats

  case class ClaimsJSON(
      azp: Option[String],
      scope: Option[String],
      `https://ndla.no/ndla_id`: Option[String],
      `https://ndla.no/user_name`: Option[String],
      permissions: Option[List[String]]
  )

  def apply(claims: JwtClaim): JWTClaims = {
    val content        = parse(claims.content).extract[ClaimsJSON]
    val oldScopes      = content.scope.map(_.split(' ').toList).getOrElse(List.empty)
    val newPermissions = content.permissions.getOrElse(List.empty)
    val mergedScopes   = (oldScopes ++ newPermissions).distinct

    new JWTClaims(
      claims.issuer,
      claims.subject,
      claims.audience,
      content.azp,
      claims.expiration,
      claims.issuedAt,
      mergedScopes,
      content.`https://ndla.no/ndla_id`,
      content.`https://ndla.no/user_name`,
      claims.jwtId
    )
  }
}
