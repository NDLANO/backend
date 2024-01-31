/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration.model

import no.ndla.search.model.LanguageValue

case class TaxonomyTranslation(
    name: String,
    language: String
) {
  def toLanguageValue: LanguageValue[String] = LanguageValue(language, name)
}

case class TaxonomySubject(
    id: String,
    name: String,
    translations: List[TaxonomyTranslation]
)

case class TaxonomyData(
    subjectsById: Map[String, TaxonomySubject]
)

object TaxonomyData {
  def from(subjects: List[TaxonomySubject]): TaxonomyData = {
    TaxonomyData(subjects.map(s => s.id -> s).toMap)
  }

  def empty: TaxonomyData = TaxonomyData(Map.empty)
}
