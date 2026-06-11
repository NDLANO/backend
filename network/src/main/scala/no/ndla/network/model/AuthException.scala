/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

sealed abstract class AuthException(val message: String, causes: Throwable*) extends RuntimeException(message) {
  causes.toList match {
    case head :: Nil  => initCause(head)
    case head :: tail =>
      initCause(head)
      tail.foreach(addSuppressed)
    case Nil => ()
  }
}

sealed abstract class UnauthenticatedException(message: String, causes: Throwable*)
    extends AuthException(message, causes*)
sealed abstract class ForbiddenException(message: String, causes: Throwable*) extends AuthException(message, causes*)
case class MissingAudienceConfiguration(issuer: String)
    extends AuthException(s"Audience for issuer $issuer was not configured for verification")
case class UnexpectedNimbusException(cause: Throwable) extends AuthException("Unexpected Nimbus exception", cause)
case class GetFeideUserWrapperException(cause: Throwable)
    extends AuthException("Failed to get Feide user wrapper", cause)

case class JwtParseException()                   extends UnauthenticatedException("Failed to parse JWT")
case class ExpiredJwtException()                 extends UnauthenticatedException("JWT is expired")
case class InvalidJwtSignatureException()        extends UnauthenticatedException("Invalid JWT signature")
case class InvalidJwtException(cause: Throwable) extends UnauthenticatedException("Failed to verify JWT", cause)
case class MissingFeideUserException()
    extends UnauthenticatedException("Failed to find Feide user for the given ID token")

case class MissingClaimException(name: String) extends ForbiddenException(s"JWT is missing claim '$name'")
case class ClaimParseException(name: String, cause: Throwable)
    extends ForbiddenException(s"Failed to parse claim '$name'", cause)
