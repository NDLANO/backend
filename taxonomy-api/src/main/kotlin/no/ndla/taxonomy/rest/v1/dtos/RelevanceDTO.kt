/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.service.dtos.TranslationDTO

@Schema(name = "Relevance")
data class RelevanceDTO(
    @field:Schema(
        description = "Specifies if node is core or supplementary",
        example = "urn:relevance:core",
    )
    val id: URI,
    @field:Schema(description = "The name of the relevance", example = "Core") val name: String,
    @field:Schema(description = "All translations of this relevance")
    val translations: Set<TranslationDTO>,
    @field:Schema(description = "List of language codes supported by translations")
    val supportedLanguages: Set<String>,
) {
  constructor(
      relevance: Relevance,
      language: String? = null,
  ) : this(
      id = relevance.id,
      name = language?.let { relevance.findTranslatedName(it) } ?: relevance.getTranslatedName(),
      translations = relevance.translations.map { TranslationDTO(it) }.toSet(),
      supportedLanguages = relevance.supportedLanguages,
  )
}
