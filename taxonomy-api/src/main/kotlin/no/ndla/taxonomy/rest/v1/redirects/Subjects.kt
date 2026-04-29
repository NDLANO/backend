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
@RequestMapping(path = ["/v1/subjects", "/v1/subjects/"])
@Deprecated("Use /v1/nodes")
class Subjects {

    @RequestMapping(value = ["", "/**"])
    fun redirect(request: HttpServletRequest): ResponseEntity<Unit> {
        val rest = request.requestURI.substringAfter("/v1/subjects").trimStart('/')
        val (path, extra) = when {
            rest.isEmpty() -> "/v1/nodes" to "nodeType=SUBJECT"
            rest == "search" -> "/v1/nodes/search" to "nodeType=SUBJECT"
            rest == "page" -> "/v1/nodes/page" to "nodeType=SUBJECT"
            rest.endsWith("/topics") -> "/v1/nodes/${rest.removeSuffix("/topics")}/nodes" to "nodeType=TOPIC"
            rest.endsWith("/resources") -> "/v1/nodes/$rest" to "recursive=true"
            else -> "/v1/nodes/$rest" to null
        }
        return permanentRedirect(path, request.queryString, extra)
    }
}
