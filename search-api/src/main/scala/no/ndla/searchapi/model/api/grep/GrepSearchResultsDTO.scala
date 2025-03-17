/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.SchemaImplicits
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class GrepSearchResultsDTO(
    @description("The total number of resources matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[GrepResultDTO]
)

object GrepSearchResultsDTO extends SchemaImplicits {
  implicit val encoder: Encoder[GrepSearchResultsDTO] = deriveEncoder
  implicit val decoder: Decoder[GrepSearchResultsDTO] = deriveDecoder
  implicit val schema: Schema[GrepSearchResultsDTO] = {
    implicit val s1: Schema["GrepLaererplanDTO"]         = Schema.string
    implicit val s2: Schema["GrepTverrfagligTemaDTO"]    = Schema.string
    implicit val s3: Schema["GrepKompetansemaalSettDTO"] = Schema.string
    implicit val s4: Schema["GrepKompetansemaalDTO"]     = Schema.string
    implicit val s5: Schema["GrepKjerneelementDTO"]      = Schema.string

    import sttp.tapir.generic.auto.*
    Schema.derived
  }
}
