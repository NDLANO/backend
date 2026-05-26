/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata

trait JwsKeySelectorFactory {
  def fromIssuer(issuerUrl: String): JWSVerificationKeySelector[Null]
}

case object DefaultJwsKeySelectorFactory extends JwsKeySelectorFactory {
  private val cacheTimeToLive     = 24 * 60 * 60 * 1000L // 1 day
  private val cacheRefreshTimeout = 15 * 1000L           // 15 seconds

  override def fromIssuer(issuerUrl: String): JWSVerificationKeySelector[Null] = {
    val issuer    = new Issuer(issuerUrl)
    val metadata  = OIDCProviderMetadata.resolve(issuer)
    val jwkSource = JWKSourceBuilder
      .create[Null](metadata.getJWKSetURI.toURL)
      .retrying(true)
      .cache(cacheTimeToLive, cacheRefreshTimeout)
      .refreshAheadCache(true)
      .outageTolerantForever()
      .rateLimited(false)
      .build();
    val algs = new java.util.HashSet(metadata.getIDTokenJWSAlgs)
    new JWSVerificationKeySelector(algs, jwkSource)
  }
}
