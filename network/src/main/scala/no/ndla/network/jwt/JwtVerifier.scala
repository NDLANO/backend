/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import com.nimbusds.jose.proc.{BadJOSEException, BadJWSException}
import com.nimbusds.jwt.proc.{DefaultJWTProcessor, ExpiredJWTException as NimbusExpiredJWTException}
import no.ndla.network.model.*

import java.text.ParseException
import scala.util.{Failure, Success, Try}

case class JwtVerifier(
    jwsKeySelectorFactory: JwsKeySelectorFactory,
    issuer: String,
    audience: Option[String],
    issuers: Set[String],
) {
  @volatile
  private var processor: Option[DefaultJWTProcessor[Null]] = None
  // Try to create processor eagerly at startup instead of on first (auth) request
  val _ = getProcessor

  def decode[A](token: String)(using decoder: JwtDecoder[A]): Try[A] =
    verify(token).flatMap(claims => decoder.decode(claims, token).toTry)

  def verify(token: String): Try[JWTClaimsSetWrapper] = process(token) match {
    case Success(claims)                       => Success(claims)
    case Failure(_: ParseException)            => Failure(JwtParseException())
    case Failure(_: NimbusExpiredJWTException) => Failure(ExpiredJwtException())
    case Failure(_: BadJWSException)           => Failure(InvalidJwsException())
    case Failure(ex: BadJOSEException)         => Failure(InvalidJoseException(ex))
    case Failure(ex)                           => Failure(UnexpectedNimbusException(ex))
  }

  private def process(token: String): Try[JWTClaimsSetWrapper] = for {
    proc <- getProcessor
    res  <- Try(JWTClaimsSetWrapper(proc.process(token, null)))
  } yield res

  private def getProcessor: Try[DefaultJWTProcessor[Null]] = processor match {
    case Some(p) => Success(p)
    case None    => synchronized {
        processor match {
          case Some(p) => Success(p)
          case None    => Try {
              val jwsKeySelector = jwsKeySelectorFactory.fromIssuer(issuer)
              val proc           = new DefaultJWTProcessor[Null]()
              proc.setJWSKeySelector(jwsKeySelector)
              proc.setJWTClaimsSetVerifier(MultiIssuerClaimsVerifier(audience, issuers))
              processor = Some(proc)
              proc
            }
        }
      }
  }
}
