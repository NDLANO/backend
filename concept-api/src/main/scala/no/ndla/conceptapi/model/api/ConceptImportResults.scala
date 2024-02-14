/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class ConceptImportResults(
    numSuccessfullyImportedConcepts: Int,
    totalAttemptedImportedConcepts: Int,
    warnings: Seq[String]
)

object ConceptImportResults {
  implicit val encoder: Encoder[ConceptImportResults] = deriveEncoder
  implicit val decoder: Decoder[ConceptImportResults] = deriveDecoder
}
