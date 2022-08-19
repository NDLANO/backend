/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.common.model.domain.{Title, VisualElement}

case class ArticleSummary(
    id: Long,
    title: Seq[Title],
    visualElement: Seq[VisualElement],
    introduction: Seq[ArticleIntroduction],
    url: String,
    license: String
)
