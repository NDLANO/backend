/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{LearningResourceType, StatusDTO}
import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.domain.concept.{Concept, ConceptMetaImage}
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableConcept(
    id: Long,
    conceptType: String,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    metaImage: Seq[ConceptMetaImage],
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    draftStatus: StatusDTO,
    users: List[String],
    updatedBy: Seq[String],
    license: Option[String],
    authors: List[String],
    created: NDLADate,
    source: Option[String],
    responsible: Option[Responsible],
    gloss: Option[String],
    domainObject: Concept,
    favorited: Long,
    learningResourceType: LearningResourceType
)

object SearchableConcept {
  implicit val encoder: Encoder[SearchableConcept] = deriveEncoder
  implicit val decoder: Decoder[SearchableConcept] = deriveDecoder
}
