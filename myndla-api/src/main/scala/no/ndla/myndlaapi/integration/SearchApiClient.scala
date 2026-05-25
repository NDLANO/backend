/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

trait SearchApiClient {
  def reindexDraft(id: String): Unit
  def reindexLearningpath(id: String): Unit
  def reindexConcept(id: String): Unit
}
