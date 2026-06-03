/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import java.net.URI
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.rest.NotFoundHttpResponseException
import no.ndla.taxonomy.rest.v1.dtos.ResolvedOldUrl
import no.ndla.taxonomy.rest.v1.dtos.UrlMapping
import no.ndla.taxonomy.service.UrlResolverService
import no.ndla.taxonomy.service.dtos.ResolvedUrl
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/url")
class UrlResolver(private val urlResolverService: UrlResolverService) {

  @GetMapping("/resolve")
  fun resolve(
      @RequestParam path: String,
      @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
      language: String,
  ): ResolvedUrl =
      urlResolverService.resolveUrl(path, language)
          ?: throw NotFoundHttpResponseException("Element with path was not found")

  @GetMapping("/mapping")
  @Operation(summary = "Returns path for an url or HTTP 404")
  fun getTaxonomyPathForUrl(
      @Parameter(
          description = "url in old rig except 'https://'",
          example = "ndla.no/nb/node/142542?fag=52253",
      )
      @RequestParam
      url: String
  ): ResolvedOldUrl =
      urlResolverService.resolveOldUrl(url)?.let { ResolvedOldUrl(it) }
          ?: throw NotFoundException(url)

  @PutMapping("/mapping")
  @Operation(
      summary = "Inserts or updates a mapping from url to nodeId and optionally subjectId",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  fun putTaxonomyNodeAndSubjectForOldUrl(@RequestBody urlMapping: UrlMapping) {
    try {
      urlResolverService.putUrlMapping(
          urlMapping.url,
          URI.create(urlMapping.nodeId),
          urlMapping.subjectId?.let { URI.create(it) },
      )
    } catch (e: UrlResolverService.NodeIdNotFoundException) {
      throw NotFoundHttpResponseException(e.message)
    }
  }
}
