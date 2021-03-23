/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.domain.{AudioMetaInformation, ReindexResult}
import no.ndla.audioapi.repository.AudioRepository

import scala.util.{Failure, Success, Try}

trait SearchIndexService {
  this: AudioRepository with IndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {}
}
