/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search.settings

import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.draft.ArticleStatus

case class MultiDraftSearchSettings(
    query: Option[String],
    noteQuery: Option[String],
    fallback: Boolean,
    language: String,
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    withIdIn: List[Long],
    subjects: List[String],
    topics: List[String],
    resourceTypes: List[String],
    learningResourceTypes: List[LearningResourceType.Value],
    supportedLanguages: List[String],
    relevanceIds: List[String],
    statusFilter: List[ArticleStatus.Value],
    userFilter: List[String],
    grepCodes: List[String],
    shouldScroll: Boolean,
    searchDecompounded: Boolean,
    aggregatePaths: List[String],
    embedResource: List[String],
    embedId: Option[String],
    includeOtherStatuses: Boolean
)
