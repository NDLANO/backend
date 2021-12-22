/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import no.ndla.articleapi.model.domain.ArticleMetaImage
import org.joda.time.DateTime

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    metaImage: Seq[ArticleMetaImage],
    tags: SearchableLanguageList,
    lastUpdated: DateTime,
    license: String,
    authors: Seq[String],
    articleType: String,
    defaultTitle: Option[String],
    grepCodes: Seq[String],
    availability: String
)
