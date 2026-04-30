/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import java.util.Optional
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO
import no.ndla.taxonomy.rest.v1.redirects.permanentRedirect
import no.ndla.taxonomy.service.NodeService
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/queries"])
class Queries(private val nodeService: NodeService) {

  @GetMapping("/{contentURI}")
  @Operation(
      summary =
          "Gets a list of contexts matching given contentURI, empty list if no matches are found.")
  @Transactional(readOnly = true)
  fun contextByContentURI(
      @PathVariable("contentURI") contentURI: Optional<URI>,
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
      language: String,
      @Parameter(
          description =
              "Whether to filter out contexts if a parent (or the node itself) is non-visible")
      @RequestParam(value = "filterVisibles", required = false, defaultValue = "true")
      filterVisibles: Boolean,
  ): List<TaxonomyContextDTO> =
      nodeService.getSearchableByContentUri(contentURI, filterVisibles, language)

  @GetMapping("/contextId")
  @Operation(
      summary =
          "Gets a list of contexts matching given contextId, empty list if no matches are found.")
  @Transactional(readOnly = true)
  fun contextByContextId(
      @RequestParam("contextId") contextId: Optional<String>,
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
      language: String,
  ): List<TaxonomyContextDTO> = nodeService.getContextByContextId(contextId, language)

  @GetMapping("/path")
  @Operation(
      summary =
          "Gets a list of contexts matching given pretty url with contextId, empty list if no matches are found.",
  )
  @Transactional(readOnly = true)
  fun queryPath(
      @RequestParam("path") path: Optional<String>,
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
      language: String,
  ): List<TaxonomyContextDTO> = nodeService.getContextByPath(path, language)

  @GetMapping("/resources")
  @Operation(summary = "DEPRECATED: Use /v1/nodes?nodeType=RESOURCE&contentURI= instead")
  @Deprecated("Use /v1/nodes?nodeType=RESOURCE&contentURI=", level = DeprecationLevel.WARNING)
  fun queryResources(request: HttpServletRequest): ResponseEntity<Unit> =
      permanentRedirect("/v1/nodes", request.queryString, "nodeType=RESOURCE")

  @GetMapping("/topics")
  @Operation(summary = "DEPRECATED: Use /v1/nodes?nodeType=TOPIC&contentURI= instead")
  @Deprecated("Use /v1/nodes?nodeType=TOPIC&contentURI=", level = DeprecationLevel.WARNING)
  fun queryTopics(request: HttpServletRequest): ResponseEntity<Unit> =
      permanentRedirect("/v1/nodes", request.queryString, "nodeType=TOPIC")
}
