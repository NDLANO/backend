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

case class TestJwsKeySelectorFactory(rsaKey: RSAKey) extends JwsKeySelectorFactory {
  private val publicJwkSet = new JWKSet(rsaKey.toPublicJWK)
  private val jwkSet       = new ImmutableJWKSet[Null](publicJwkSet)

  override def fromIssuer(issuerUrl: String): JWSVerificationKeySelector[Null] =
    new JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSet)
}
