/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import java.net.URI
import no.ndla.taxonomy.service.NodeService
import no.ndla.taxonomy.service.QualityEvaluationService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/admin"])
class Admin(
    private val nodeService: NodeService,
    private val qualityEvaluationService: QualityEvaluationService,
) {

  @GetMapping("/buildContexts")
  @Operation(
      summary = "Updates contexts for all roots. Requires taxonomy:admin access.",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
  fun buildAllContexts() = nodeService.buildAllContextsAsync()

  @PostMapping("/buildAverageTree/{id}")
  @Operation(
      summary = "Updates average tree for the provided node. Requires taxonomy:admin access.",
      security = [SecurityRequirement(name = "oauth")],
  )
  @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
  fun buildAverageTree(@PathVariable("id") id: URI) =
      qualityEvaluationService.updateEntireAverageTreeForNode(id)

  @PostMapping("/buildAverageTree")
  @Operation(
      summary = "Updates average tree for all nodes. Requires taxonomy:admin access.",
      security = [SecurityRequirement(name = "oauth")],
  )
  @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
  fun buildAverageTree() = qualityEvaluationService.updateQualityEvaluationOfAllNodes()
}
