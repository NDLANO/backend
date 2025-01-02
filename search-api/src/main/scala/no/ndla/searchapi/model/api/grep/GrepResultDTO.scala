/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import no.ndla.searchapi.model.api.TitleDTO
import sttp.tapir.Schema.annotations.description

@description("Information about a single grep search result entry")
sealed trait GrepResultDTO {
  @description("The grep code") val code: String
  @description("The greps title") val title: TitleDTO
}

object GrepResultDTO {
  implicit val encoder: Encoder[GrepResultDTO] = Encoder.instance { case x: GrepKjerneelementDTO =>
    x.asJson
  }
  implicit val decoder: Decoder[GrepResultDTO] = List[Decoder[GrepResultDTO]](
    Decoder[GrepKjerneelementDTO].widen
  ).reduceLeft(_ or _)
}

case class GrepKjerneelementDTO(
    code: String,
    title: TitleDTO,
    laereplanCode: Option[String]
) extends GrepResultDTO
