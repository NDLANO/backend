/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import java.net.URI
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/relevances", "/v1/relevances/"])
class Relevances {

  @GetMapping
  @Operation(summary = "Gets all relevances")
  @Transactional(readOnly = true)
  fun getAllRelevances(
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(
          value = "language",
          required = false,
      )
      language: String?
  ): List<RelevanceDTO> = Relevance.entries.map { RelevanceDTO(it, language) }

  @GetMapping("/{id}")
  @Operation(
      summary = "Gets a single relevance",
      description =
          "Default language will be returned if desired language not found or if parameter is omitted.",
  )
  @Transactional(readOnly = true)
  fun getRelevance(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(
          value = "language",
          required = false,
      )
      language: String?,
  ): RelevanceDTO = RelevanceDTO(Relevance.getRelevance(id), language)
}
