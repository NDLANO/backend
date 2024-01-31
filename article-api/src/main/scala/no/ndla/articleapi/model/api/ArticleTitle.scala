/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Description of a title")
case class ArticleTitle(
    @description("The freetext title of the article") title: String,
    @description("The freetext html-version title of the article") htmlTitle: String,
    @description("ISO 639-1 code that represents the language used in title") language: String
)
