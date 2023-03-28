/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.common.model.domain.{ArticleMetaImage, Responsible}
import no.ndla.common.model.domain.draft.{Draft, RevisionMeta}
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

import java.time.LocalDateTime

case class SearchableDraft(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: LocalDateTime,
    license: Option[String],
    authors: List[String],
    articleType: String,
    metaImage: List[ArticleMetaImage],
    defaultTitle: Option[String],
    supportedLanguages: List[String],
    notes: List[String],
    contexts: List[SearchableTaxonomyContext],
    draftStatus: Status,
    users: List[String],
    previousVersionsNotes: List[String],
    grepContexts: List[SearchableGrepContext],
    traits: List[String],
    embedAttributes: SearchableLanguageList,
    embedResourcesAndIds: List[EmbedValues],
    revisionMeta: List[RevisionMeta],
    nextRevision: Option[RevisionMeta],
    responsible: Option[Responsible],
    domainObject: Draft
)
