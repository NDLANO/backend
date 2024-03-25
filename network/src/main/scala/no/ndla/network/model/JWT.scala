/*
 * Part of NDLA network.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
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

case class ClaimsJSON(
    azp: Option[String],
    scope: Option[String],
    `https://ndla.no/ndla_id`: Option[String],
    `https://ndla.no/user_name`: Option[String],
    permissions: Option[List[String]]
)

object ClaimsJSON {
  implicit val encoder: Encoder[ClaimsJSON] = deriveEncoder
  implicit val decoder: Decoder[ClaimsJSON] = deriveDecoder
}

object JWTClaims {
  def empty(): JWTClaims = {
    new JWTClaims(
      iss = None,
      sub = None,
      aud = None,
      azp = None,
      exp = None,
      iat = None,
      scope = List.empty,
      ndla_id = None,
      user_name = None,
      jti = None
    )

  }

  def apply(claims: JwtClaim): JWTClaims = {
    val content        = CirceUtil.unsafeParseAs[ClaimsJSON](claims.content)
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
