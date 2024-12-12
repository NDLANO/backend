/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller.parameters

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Input parameters to subject aggregations endpoint")
case class SubjectAggsInputDTO(
    @description("A comma separated list of subjects the learning resources should be filtered by.")
    subjects: Option[List[String]]
)

object SubjectAggsInputDTO {
  implicit val encoder: Encoder[SubjectAggsInputDTO] = deriveEncoder
  implicit val decoder: Decoder[SubjectAggsInputDTO] = deriveDecoder
}
