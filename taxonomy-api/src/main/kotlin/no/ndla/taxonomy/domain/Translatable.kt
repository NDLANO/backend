/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.util.Objects
import java.util.Optional

interface Translatable {

  var translations: MutableList<JsonTranslation>

  fun clearTranslations() {
    translations = ArrayList()
  }

  fun getTranslation(languageCode: String): Optional<JsonTranslation> {
    return translations
        .stream()
        .filter { translation -> translation.languageCode == languageCode }
        .findFirst()
  }

  fun addTranslation(nodeTranslation: JsonTranslation): JsonTranslation {
    if (nodeTranslation.parent !== this) {
      nodeTranslation.parent = this
    }
    val newTranslations =
        ArrayList(
            translations
                .stream()
                .filter { t -> !Objects.equals(t.languageCode, nodeTranslation.languageCode) }
                .toList())
    newTranslations.add(nodeTranslation)
    translations = newTranslations
    return nodeTranslation
  }

  fun addTranslation(name: String, languageCode: String): JsonTranslation {
    val nodeTranslation = JsonTranslation(name, languageCode)
    return this.addTranslation(nodeTranslation)
  }

  fun removeTranslation(translation: JsonTranslation) {
    translation.parent = null
    val newTranslations =
        ArrayList(translations.stream().filter { t -> t !== translation }.toList())
    translations = newTranslations
  }

  fun removeTranslation(languageCode: String) {
    getTranslation(languageCode).ifPresent { this.removeTranslation(it) }
  }
}
