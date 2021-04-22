/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.repository.AudioRepository

trait SearchIndexService {
  this: AudioRepository with AudioIndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {}
}
