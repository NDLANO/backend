/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

case class ConceptImportResults(
    numSuccessfullyImportedConcepts: Long,
    totalAttemptedImportedConcepts: Long,
    warnings: Seq[String]
)
