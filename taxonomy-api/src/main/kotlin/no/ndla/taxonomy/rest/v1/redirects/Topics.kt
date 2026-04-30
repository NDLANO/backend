/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.redirects

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/topics", "/v1/topics/"])
@Deprecated("Use /v1/nodes")
class Topics {

  @RequestMapping(value = ["", "/**"])
  fun redirect(request: HttpServletRequest): ResponseEntity<Unit> {
    val rest = request.requestURI.substringAfter("/v1/topics").trimStart('/')
    val (path, extra) =
        when {
          rest.isEmpty() -> "/v1/nodes" to "nodeType=TOPIC"
          rest == "search" -> "/v1/nodes/search" to "nodeType=TOPIC"
          rest == "page" -> "/v1/nodes/page" to "nodeType=TOPIC"
          rest.endsWith("/topics") ->
              "/v1/nodes/${rest.removeSuffix("/topics")}/nodes" to "nodeType=TOPIC"
          else -> "/v1/nodes/$rest" to null
        }
    return permanentRedirect(path, request.queryString, extra)
  }
}
