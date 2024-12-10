/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class ConceptImportResultsDTO(
    numSuccessfullyImportedConcepts: Int,
    totalAttemptedImportedConcepts: Int,
    warnings: Seq[String]
)

object ConceptImportResultsDTO {
  implicit val encoder: Encoder[ConceptImportResultsDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptImportResultsDTO] = deriveDecoder
}
