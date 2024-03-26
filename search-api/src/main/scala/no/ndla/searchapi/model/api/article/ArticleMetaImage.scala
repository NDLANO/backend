/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.language.model.LanguageField
import sttp.tapir.Schema.annotations.description

@description("Meta description of the article")
case class ArticleMetaImage(
    @description("The meta image url") url: String,
    @description("The alt text for the meta image") alt: String,
    @description(
      "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
) extends LanguageField[String] {
  override def value: String    = url
  override def isEmpty: Boolean = url.isEmpty || alt.isEmpty
}
