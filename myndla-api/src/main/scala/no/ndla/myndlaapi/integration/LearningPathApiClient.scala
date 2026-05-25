/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import no.ndla.common.model.api.learningpath.LearningPathStatsDTO

import scala.util.Try

trait LearningPathApiClient {
  def getStats: Try[LearningPathStatsDTO]
}
