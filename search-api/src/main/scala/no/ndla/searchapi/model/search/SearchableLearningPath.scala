/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.LearningResourceType
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.api.learningpath.CopyrightDTO
import no.ndla.common.model.domain.{Priority, Responsible, RevisionMeta}

case class SearchableLearningPath(
    domainObject: LearningPath,
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    description: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    coverPhotoId: Option[String],
    duration: Option[Int],
    status: String,
    draftStatus: SearchableStatus,
    owner: String,
    users: List[String],
    verificationStatus: String,
    lastUpdated: NDLADate,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    learningsteps: List[SearchableLearningStep],
    license: String,
    copyright: CopyrightDTO,
    isBasedOn: Option[Long],
    supportedLanguages: List[String],
    authors: List[String],
    context: Option[SearchableTaxonomyContext],
    contexts: List[SearchableTaxonomyContext],
    contextids: List[String],
    favorited: Long,
    learningResourceType: LearningResourceType,
    typeName: List[String],
    priority: Priority,
    defaultParentTopicName: Option[String],
    parentTopicName: SearchableLanguageValues,
    defaultRoot: Option[String],
    primaryRoot: SearchableLanguageValues,
    resourceTypeName: SearchableLanguageValues,
    defaultResourceTypeName: Option[String],
    revisionMeta: List[RevisionMeta],
    nextRevision: Option[RevisionMeta],
    grepCodes: List[String],
    responsible: Option[Responsible]
)

object SearchableLearningPath {
  implicit val encoder: Encoder[SearchableLearningPath] = deriveEncoder
  implicit val decoder: Decoder[SearchableLearningPath] = deriveDecoder
}
