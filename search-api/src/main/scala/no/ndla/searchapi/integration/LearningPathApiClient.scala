/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient

trait LearningPathApiClient {
  this: NdlaClient with SearchApiClient =>
  val learningPathApiClient: LearningPathApiClient

  class LearningPathApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath     = "learningpath-api/v2/learningpaths"
    override val name           = "learningpaths"
    override val dumpDomainPath = "intern/dump/learningpath"
  }
}
