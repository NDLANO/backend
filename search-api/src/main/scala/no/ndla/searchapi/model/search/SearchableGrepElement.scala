/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.search.model.SearchableLanguageValues

case class SearchableGrepElement(
    code: String,
    title: SearchableLanguageValues,
    defaultTitle: Option[String]
)

object SearchableGrepElement {
  implicit def encoder: Encoder[SearchableGrepElement] = deriveEncoder
  implicit def decoder: Decoder[SearchableGrepElement] = deriveDecoder
}
