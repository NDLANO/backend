/*
 * Part of NDLA network.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.jwt

import javax.servlet.http.HttpServletRequest
import no.ndla.network.model.{JWTClaims, NdlaHttpRequest}
import pdi.jwt.{JwtJson4s, JwtOptions}

import scala.util.{Failure, Properties, Success}

class JWTExtractor(request: NdlaHttpRequest) {

  private val jwtClaims = request
    .getHeader("Authorization")
    .flatMap(authHeader => {
      val jwt = authHeader.replace("Bearer ", "")
      // Leaning on token validation being done somewhere else...
      JwtJson4s.decode(jwt, JwtOptions(signature = false, expiration = false)) match {
        case Success(claims) => Some(JWTClaims(claims))
        case Failure(_)      => None
      }
    })

  def extractUserId(): Option[String] = jwtClaims.flatMap(_.ndla_id)

  def extractUserRoles(): List[String] = {
    val rawRoles = jwtClaims.map(_.scope).getOrElse(List.empty)
    val env = Properties.envOrElse("NDLA_ENVIRONMENT", "local") match {
      case "local" => "test"
      case x       => x
    }
    val envSuffix = s"-$env:"
    // Handle both types of role formats. Both with and without env-suffix.
    // Support for old role formats will be removed in one of the next releases
    rawRoles.filter(r => r.contains(envSuffix) || !r.contains("-")).map(_.replace(envSuffix, ":"))
  }

  def extractUserName(): Option[String] = jwtClaims.flatMap(_.user_name)

  def extractClientId(): Option[String] = jwtClaims.flatMap(_.azp)

}
