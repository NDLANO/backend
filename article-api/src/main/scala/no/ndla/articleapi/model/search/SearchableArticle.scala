/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import no.ndla.common.model.domain.ArticleMetaImage
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

import java.time.LocalDateTime

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    metaImage: Seq[ArticleMetaImage],
    tags: SearchableLanguageList,
    lastUpdated: LocalDateTime,
    license: String,
    authors: Seq[String],
    articleType: String,
    defaultTitle: Option[String],
    grepCodes: Seq[String],
    availability: String
)
