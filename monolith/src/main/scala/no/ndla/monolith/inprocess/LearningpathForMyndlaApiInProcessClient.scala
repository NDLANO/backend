/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.common.model.api.learningpath.LearningPathStatsDTO
import no.ndla.myndlaapi.integration.LearningPathApiClient

import scala.util.Try

/** In-process implementation of myndla-api's [[LearningPathApiClient]] trait that delegates to learningpath-api's
  * [[no.ndla.learningpathapi.service.ReadService]] directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.learningpathapi.ComponentRegistry]] and [[no.ndla.myndlaapi.ComponentRegistry]] in the monolith.
  */
class LearningpathForMyndlaApiInProcessClient(learningpathApiCr: => no.ndla.learningpathapi.ComponentRegistry)
    extends LearningPathApiClient {

  override def getStats: Try[LearningPathStatsDTO] = Try(learningpathApiCr.readService.getLearningPathStats)
}
