/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Description of the article introduction")
case class ArticleIntroduction(
    @description("The introduction content") introduction: String,
    @description("The html-version introduction content") htmlIntroduction: String,
    @description(
      "The ISO 639-1 language code describing which article translation this introduction belongs to"
    ) language: String
)
