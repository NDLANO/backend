/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.util.Optional

interface Translatable {

  var translations: MutableList<JsonTranslation>

  fun clearTranslations() {
    translations = mutableListOf()
  }

  fun getTranslation(languageCode: String): Optional<JsonTranslation> =
      Optional.ofNullable(translations.firstOrNull { it.languageCode == languageCode })

  fun addTranslation(nodeTranslation: JsonTranslation): JsonTranslation {
    if (nodeTranslation.parent !== this) {
      nodeTranslation.parent = this
    }
    translations =
        (translations.filter { it.languageCode != nodeTranslation.languageCode } + nodeTranslation)
            .toMutableList()
    return nodeTranslation
  }

  fun addTranslation(name: String, languageCode: String): JsonTranslation =
      addTranslation(JsonTranslation(name, languageCode))

  fun removeTranslation(translation: JsonTranslation) {
    translation.parent = null
    translations = translations.filter { it !== translation }.toMutableList()
  }

  fun removeTranslation(languageCode: String) {
    getTranslation(languageCode).ifPresent(::removeTranslation)
  }
}
