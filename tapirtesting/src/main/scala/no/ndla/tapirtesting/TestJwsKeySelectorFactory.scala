/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.{JWKSet, RSAKey}
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import no.ndla.network.jwt.JwsKeySelectorFactory

import scala.io.Source

case object TestJwsKeySelectorFactory extends JwsKeySelectorFactory {
  private val classLoader  = getClass.getClassLoader
  private val json         = Source.fromResource("test-jwks.json", classLoader).mkString
  private val rsaJwk       = RSAKey.parse(json)
  private val publicJwkSet = new JWKSet(rsaJwk.toPublicJWK)
  private val jwkSet       = new ImmutableJWKSet[Null](publicJwkSet)

  override def fromIssuer(issuerUrl: String): JWSVerificationKeySelector[Null] =
    new JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSet)
}
