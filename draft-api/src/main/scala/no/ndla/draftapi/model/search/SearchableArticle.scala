/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import no.ndla.common.model.NDLADate
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    license: Option[String],
    authors: Seq[String],
    articleType: String,
    notes: Seq[String],
    defaultTitle: Option[String],
    users: Seq[String],
    previousNotes: Seq[String],
    grepCodes: Seq[String],
    status: SearchableStatus
)
