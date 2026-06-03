/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.redirects

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/topics/{id}/translations", "/v1/topics/{id}/translations/"])
@Deprecated("Use /v1/nodes/{id}/translations")
class TopicTranslations {
  @RequestMapping(value = ["", "/**"])
  fun redirect(
      request: HttpServletRequest,
      @PathVariable(name = "id") id: String
  ): ResponseEntity<Unit> {
    val rest = request.requestURI.substringAfter("/translations")
    return permanentRedirect("/v1/nodes/$id/translations$rest", request.queryString)
  }
}
