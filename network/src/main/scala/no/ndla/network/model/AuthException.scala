/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

sealed abstract class AuthException(val message: String)        extends RuntimeException(message)
sealed abstract class UnauthenticatedException(message: String) extends AuthException(message)
sealed abstract class ForbiddenException(message: String)       extends AuthException(message)
case class UnexpectedNimbusException(cause: Throwable)          extends AuthException("Unexpected Nimbus exception") {
  initCause(cause)
}

case class JwtParseException()                    extends UnauthenticatedException("Failed to parse JWT")
case class ExpiredJwtException()                  extends UnauthenticatedException("JWT is expired")
case class InvalidJwsException()                  extends UnauthenticatedException("Invalid JWS")
case class InvalidJoseException(cause: Throwable) extends UnauthenticatedException("Invalid JOSE object") {
  initCause(cause)
}

case class MissingClaimException(name: String) extends ForbiddenException(s"JWT is missing claim '$name'")
case class ClaimParseException(name: String, cause: Throwable)
    extends ForbiddenException(s"Failed to parse claim '$name'") {
  initCause(cause)
}
