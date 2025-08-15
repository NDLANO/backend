/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import no.ndla.common.model.domain.draft
import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information about articles")
case class ArticleDomainDumpDTO(
    @description("The total number of articles in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[draft.Draft]
)

object ArticleDomainDumpDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}
  import sttp.tapir.generic.auto.*
  import sttp.tapir.Schema

  implicit val encoder: Encoder[ArticleDomainDumpDTO] = deriveEncoder
  implicit val decoder: Decoder[ArticleDomainDumpDTO] = deriveDecoder
  implicit def schema: Schema[ArticleDomainDumpDTO]   = DeriveHelpers.getSchema
}
