/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Id for a single Article")
case class ArticleIdV2DTO(
    @description("The unique id of the article")
    id: Long
)
