/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SearchableLearningStep(stepType: String)

object SearchableLearningStep {
  implicit val encoder: Encoder[SearchableLearningStep] = deriveEncoder
  implicit val decoder: Decoder[SearchableLearningStep] = deriveDecoder
}
