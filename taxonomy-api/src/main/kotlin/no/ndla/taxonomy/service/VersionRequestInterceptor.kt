/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.AsyncHandlerInterceptor

/**
 * Interceptor which extracts http header and places a database schema name in the VersionContext
 * object.
 */
@Component
class VersionRequestInterceptor(private val versionHeaderExtractor: VersionHeaderExtractor) :
    AsyncHandlerInterceptor {

  override fun preHandle(
      request: HttpServletRequest,
      response: HttpServletResponse,
      handler: Any,
  ): Boolean {
    if (request.requestURI.startsWith("/v1/versions")) {
      VersionContext.clear()
    }
    VersionContext.setCurrentVersion(versionHeaderExtractor.getVersionSchemaFromHeader(request))
    return true
  }

  override fun afterCompletion(
      request: HttpServletRequest,
      response: HttpServletResponse,
      handler: Any,
      ex: Exception?,
  ) {
    VersionContext.clear()
  }
}
