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
case class SubjectAggsInput(
    @description("A comma separated list of subjects the learning resources should be filtered by.")
    subjects: Option[List[String]]
)

object SubjectAggsInput {
  implicit val encoder: Encoder[SubjectAggsInput] = deriveEncoder
  implicit val decoder: Decoder[SubjectAggsInput] = deriveDecoder
}
