/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.redirects

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/resources", "/v1/resources/"])
@Deprecated("Use /v1/nodes")
class Resources {

    @RequestMapping(value = ["", "/**"])
    fun redirect(request: HttpServletRequest): ResponseEntity<Unit> {
        val rest = request.requestURI.substringAfter("/v1/resources").trimStart('/')
        val (path, extra) = when {
            rest.isEmpty() -> "/v1/nodes" to "nodeType=RESOURCE"
            rest == "search" -> "/v1/nodes/search" to "nodeType=RESOURCE"
            rest == "page" -> "/v1/nodes/page" to "nodeType=RESOURCE"
            else -> "/v1/nodes/$rest" to null
        }
        return permanentRedirect(path, request.queryString, extra)
    }
}
