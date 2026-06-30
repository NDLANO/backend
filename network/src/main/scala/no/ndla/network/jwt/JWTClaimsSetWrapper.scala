/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import com.nimbusds.jwt.JWTClaimsSet
import no.ndla.network.model.{ClaimParseException, ForbiddenException, MissingClaimException}

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

opaque type JWTClaimsSetWrapper = JWTClaimsSet

object JWTClaimsSetWrapper {
  def apply(claims: JWTClaimsSet): JWTClaimsSetWrapper = claims

  extension (claims: JWTClaimsSetWrapper) {
    def stringListClaim(name: String): Either[ForbiddenException, List[String]] =
      doSafe(name)(_.getStringArrayClaim.andThen(_.toList))
    def stringClaim(name: String): Either[ForbiddenException, String] = doSafe(name)(_.getStringClaim)
    def longClaim(name: String): Either[ForbiddenException, Long]     = doSafe(name)(_.getLongClaim)

    def issueTime: Either[ForbiddenException, Long] = Option(claims.getIssueTime)
      .map(_.toInstant.getEpochSecond)
      .toRight(MissingClaimException("iat"))

    def expirationTime: Either[ForbiddenException, Long] = Option(claims.getExpirationTime)
      .map(_.toInstant.getEpochSecond)
      .toRight(MissingClaimException("exp"))

    def audience: Either[ForbiddenException, List[String]] = claims.getAudience.asScala.toList match {
      case Nil => Left(MissingClaimException("aud"))
      case l   => Right(l)
    }

    private def doSafe[A](name: String)(f: JWTClaimsSet => String => A): Either[ForbiddenException, A] =
      Try(Option(f(claims)(name))) match {
        case Success(Some(a)) => Right(a)
        case Success(None)    => Left(MissingClaimException(name))
        case Failure(ex)      => Left(ClaimParseException(name, ex))
      }
  }
}
