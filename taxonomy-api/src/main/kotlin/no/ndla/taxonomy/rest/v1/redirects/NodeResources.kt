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
@RequestMapping(path = ["/v1/node-resources", "/v1/node-resources/"])
@Deprecated("Use /v1/node-connections")
class NodeResources {

    @RequestMapping(value = ["", "/**"])
    fun redirect(request: HttpServletRequest): ResponseEntity<Unit> {
        val rest = request.requestURI.substringAfter("/v1/node-resources").trimStart('/')
        val path = if (rest.isEmpty()) "/v1/node-connections" else "/v1/node-connections/$rest"
        return permanentRedirect(path, request.queryString)
    }
}
