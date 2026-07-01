/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration

import org.slf4j.LoggerFactory
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestInitializer
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Gets the Authorization header from the incoming request and passes it along to the outgoing
 * request.
 */
class AuthorizationRequestInitializer : ClientHttpRequestInitializer {
  override fun initialize(request: ClientHttpRequest) {
    val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
    if (attrs == null) {
      logger.warn("No incoming request found in context, skipping authorization header")
      return
    }

    val authHeader = attrs.request.getHeader("Authorization")
    if (!authHeader.isNullOrEmpty()) {
      request.headers.add("Authorization", authHeader)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(AuthorizationRequestInitializer::class.java)
  }
}
