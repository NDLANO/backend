/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information about an author")
case class Author(
    @description("The description of the author. Eg. Photographer or Supplier") `type`: String,
    @description("The name of the of the author") name: String
)
