/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.search.SearchableLanguageValues
import no.ndla.common.model.domain.frontpage.SubjectPage

case class SearchableSubjectPage(
    id: Long,
    name: String,
    aboutTitle: SearchableLanguageValues,
    aboutDescription: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    domainObject: SubjectPage,
)

object SearchableSubjectPage {
  implicit val encoder: Encoder[SearchableSubjectPage] = deriveEncoder
  implicit val decoder: Decoder[SearchableSubjectPage] = deriveDecoder
}
