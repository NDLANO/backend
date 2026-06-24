/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.net.URI
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.exceptions.NotFoundException

private fun relevanceTranslations(en: String, nb: String, nn: String, se: String) =
    listOf(
        JsonTranslation(en, "en"),
        JsonTranslation(nb, "nb"),
        JsonTranslation(nn, "nn"),
        JsonTranslation(se, "se"),
    )

enum class Relevance(
    val id: URI,
    val translations: List<JsonTranslation>,
) {
  CORE(
      id = URI.create("urn:relevance:core"),
      translations =
          relevanceTranslations(
              en = "Core content",
              nb = "Kjernestoff",
              nn = "Kjernestoff",
              se = "Guovddášávnnas",
          ),
  ),
  SUPPLEMENTARY(
      id = URI.create("urn:relevance:supplementary"),
      translations =
          relevanceTranslations(
              en = "Supplementary content",
              nb = "Tilleggsstoff",
              nn = "Tilleggstoff",
              se = "Lassiávnnas",
          ),
  ),
  ;

  @Throws(NoSuchElementException::class)
  fun getTranslatedName() =
      this.translations.first { it.languageCode == Constants.DefaultLanguage }.name
          ?: throw NoSuchElementException()

  fun findTranslatedName(language: String) = translations.find { it.languageCode == language }?.name

  val supportedLanguages = translations.mapNotNull { it.languageCode }.toSet()

  companion object {
    private val BY_ID = entries.associateBy { it.id }

    fun findRelevanceById(id: URI) = BY_ID[id]

    @Throws(NotFoundException::class)
    fun getRelevance(id: URI) = BY_ID[id] ?: throw NotFoundException("Relevance", id)
  }
}
