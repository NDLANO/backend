/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.Responsible
import no.ndla.search.model.domain.EmbedValues
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.common.model.domain.concept.Concept

case class SearchableConcept(
    id: Long,
    conceptType: String,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    status: Status,
    updatedBy: Seq[String],
    license: Option[String],
    copyright: Option[SearchableCopyright],
    embedResourcesAndIds: List[EmbedValues],
    visualElement: SearchableLanguageValues,
    created: NDLADate,
    source: Option[String],
    responsible: Option[Responsible],
    gloss: Option[String],
    domainObject: Concept,
    sortableConceptType: SearchableLanguageValues,
    defaultSortableConceptType: Option[String]
)

object SearchableConcept {
  implicit val encoder: Encoder[SearchableConcept] = deriveEncoder
  implicit val decoder: Decoder[SearchableConcept] = deriveDecoder
}
