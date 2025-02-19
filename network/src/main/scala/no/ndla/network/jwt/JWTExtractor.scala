/*
 * Part of NDLA network
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import no.ndla.network.model.{JWTClaims, NdlaHttpRequest}
import pdi.jwt.{JwtCirce, JwtOptions}

import scala.util.{Failure, Success}

class JWTExtractor(jwtClaims: Option[JWTClaims]) {
  def extractUserId(): Option[String] = jwtClaims.flatMap(_.ndla_id)

  def extractUserRoles(): List[String] = jwtClaims.map(_.scope).getOrElse(List.empty)

  def extractUserName(): Option[String] = jwtClaims.flatMap(_.user_name)

  def extractClientId(): Option[String] = jwtClaims.flatMap(_.azp)

}

object JWTExtractor {
  def apply(request: NdlaHttpRequest): JWTExtractor = {
    val jwtClaims = request
      .getHeader("Authorization")
      .flatMap(authHeader => {
        val jwt = authHeader.replace("Bearer ", "")
        // Leaning on token validation being done somewhere else...
        JwtCirce.decode(jwt, JwtOptions(signature = false, expiration = false)) match {
          case Success(claims) => Some(JWTClaims(claims))
          case Failure(_)      => None
        }
      })
    new JWTExtractor(jwtClaims)
  }

  def apply(token: String): JWTExtractor = {
    val claims = JwtCirce.decode(token, JwtOptions(signature = false, expiration = false)) match {
      case Failure(_)      => None
      case Success(claims) => Some(JWTClaims(claims))
    }

    new JWTExtractor(claims)
  }

}
