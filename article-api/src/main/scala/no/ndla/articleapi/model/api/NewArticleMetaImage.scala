/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.api
import sttp.tapir.Schema.annotations.description

case class NewArticleMetaImage(
    @description("The image-api id of the meta image") id: String,
    @description("The alt text of the meta image") alt: String
)
