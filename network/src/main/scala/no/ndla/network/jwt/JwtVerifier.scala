/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import com.nimbusds.jose.proc.{BadJOSEException, BadJWSException, JWSVerificationKeySelector}
import com.nimbusds.jwt.proc.{DefaultJWTProcessor, ExpiredJWTException as NimbusExpiredJWTException}
import no.ndla.network.model.{ExpiredJwtException, InvalidJoseException, InvalidJwsException, UnexpectedNimbusException}

import scala.util.{Failure, Success, Try}

case class JwtVerifier(jwsKeySelector: JWSVerificationKeySelector[Null]) {
  def decode[A](token: String)(using decoder: JwtDecoder[A]): Try[A] =
    verify(token).flatMap(claims => decoder.decode(claims, token).toTry)

  def verify(token: String): Try[JWTClaimsSetWrapper] = process(token) match {
    case Success(claims)                       => Success(claims)
    case Failure(_: NimbusExpiredJWTException) => Failure(ExpiredJwtException())
    case Failure(_: BadJWSException)           => Failure(InvalidJwsException())
    case Failure(ex: BadJOSEException)         => Failure(InvalidJoseException(ex))
    case Failure(ex)                           => Failure(UnexpectedNimbusException(ex))
  }

  private def process(token: String): Try[JWTClaimsSetWrapper] =
    Try(JWTClaimsSetWrapper(processor.process(token, null)))

  private val processor = {
    val processor = new DefaultJWTProcessor[Null]()
    processor.setJWSKeySelector(jwsKeySelector)
    processor
  }
}
