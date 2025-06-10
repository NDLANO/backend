/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.repository.AudioRepository

trait SearchIndexService {
  this: AudioRepository & AudioIndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends StrictLogging {}
}
