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
@RequestMapping(path = ["/v1/subject-topics", "/v1/subject-topics/"])
@Deprecated("Use /v1/node-connections")
class SubjectTopics {

    @RequestMapping(value = ["", "/**"])
    fun redirect(request: HttpServletRequest): ResponseEntity<Unit> {
        val rest = request.requestURI.substringAfter("/v1/subject-topics").trimStart('/')
        val path = if (rest.isEmpty()) "/v1/node-connections" else "/v1/node-connections/$rest"
        return permanentRedirect(path, request.queryString)
    }
}
