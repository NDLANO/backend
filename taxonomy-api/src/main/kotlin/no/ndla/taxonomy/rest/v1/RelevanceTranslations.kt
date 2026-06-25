/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import java.net.URI
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.service.dtos.TranslationDTO
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Deprecated("Use /v1/relevances")
@RequestMapping(path = ["/v1/relevances/{id}/translations", "/v1/relevances/{id}/translations/"])
class RelevanceTranslations {

  @GetMapping
  @Operation(summary = "Gets all relevanceTranslations for a single relevance")
  @Transactional(readOnly = true)
  fun getAllRelevanceTranslations(@PathVariable("id") id: URI): List<TranslationDTO> =
      Relevance.getRelevance(id).translations.map { TranslationDTO(it) }

  @GetMapping("/{language}")
  @Operation(summary = "Gets a single translation for a single relevance")
  @Transactional(readOnly = true)
  fun getRelevanceTranslation(
      @PathVariable("id") id: URI,
      @PathVariable("language") language: String,
  ): TranslationDTO {
    return Relevance.getRelevance(id)
        .translations
        .firstOrNull { it.languageCode == language }
        ?.let { TranslationDTO(it) }
        ?: throw NotFoundException("translation with language code $language for relevance $id")
  }
}
