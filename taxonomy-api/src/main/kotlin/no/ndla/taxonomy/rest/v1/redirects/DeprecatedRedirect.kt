/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.redirects

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

internal fun permanentRedirect(newPath: String, queryString: String?, extraParam: String? = null): ResponseEntity<Unit> {
    val parts = listOfNotNull(queryString?.takeIf { it.isNotEmpty() }, extraParam)
    val target = if (parts.isEmpty()) newPath else "$newPath?${parts.joinToString("&")}"
    return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
        .location(URI.create(target))
        .build()
}
