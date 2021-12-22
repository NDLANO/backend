/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.integration.SearchApiClient

trait SearchClients {
  this: SearchApiClient =>
  val SearchClients: Map[String, SearchApiClient]
}
