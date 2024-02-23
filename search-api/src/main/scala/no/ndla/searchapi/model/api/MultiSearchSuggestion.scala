/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about search-suggestions")
case class MultiSearchSuggestion(
    @description("The name of the field suggested for") name: String,
    @description("The list of suggestions for given field") suggestions: Seq[SearchSuggestion]
)

object MultiSearchSuggestion {
  implicit val encoder: Encoder[MultiSearchSuggestion] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchSuggestion] = deriveDecoder
}

@description("Search suggestion for query-text")
case class SearchSuggestion(
    @description("The search query suggestions are made for") text: String,
    @description("The offset in the search query") offset: Int,
    @description("The position index in the search query") length: Int,
    @description("The list of suggest options for the field") options: Seq[SuggestOption]
)

object SearchSuggestion {
  implicit val encoder: Encoder[SearchSuggestion] = deriveEncoder
  implicit val decoder: Decoder[SearchSuggestion] = deriveDecoder
}

@description("Search suggestion options for the terms in the query")
case class SuggestOption(
    @description("The suggested text") text: String,
    @description("The score of the suggestion") score: Double
)

object SuggestOption {
  implicit val encoder: Encoder[SuggestOption] = deriveEncoder
  implicit val decoder: Decoder[SuggestOption] = deriveDecoder
}
