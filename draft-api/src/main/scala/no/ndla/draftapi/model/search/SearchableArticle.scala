/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import org.joda.time.DateTime

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: DateTime,
    license: Option[String],
    authors: Seq[String],
    articleType: String,
    notes: Seq[String],
    defaultTitle: Option[String],
    users: Seq[String],
    previousNotes: Seq[String],
    grepCodes: Seq[String]
)
