/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about search-suggestions")
case class MultiSearchSuggestionDTO(
    @description("The name of the field suggested for") name: String,
    @description("The list of suggestions for given field") suggestions: Seq[SearchSuggestionDTO]
)

object MultiSearchSuggestionDTO {
  implicit val encoder: Encoder[MultiSearchSuggestionDTO] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchSuggestionDTO] = deriveDecoder
}

@description("Search suggestion for query-text")
case class SearchSuggestionDTO(
    @description("The search query suggestions are made for") text: String,
    @description("The offset in the search query") offset: Int,
    @description("The position index in the search query") length: Int,
    @description("The list of suggest options for the field") options: Seq[SuggestOptionDTO]
)

object SearchSuggestionDTO {
  implicit val encoder: Encoder[SearchSuggestionDTO] = deriveEncoder
  implicit val decoder: Decoder[SearchSuggestionDTO] = deriveDecoder
}

@description("Search suggestion options for the terms in the query")
case class SuggestOptionDTO(
    @description("The suggested text") text: String,
    @description("The score of the suggestion") score: Double
)

object SuggestOptionDTO {
  implicit val encoder: Encoder[SuggestOptionDTO] = deriveEncoder
  implicit val decoder: Decoder[SuggestOptionDTO] = deriveDecoder
}
