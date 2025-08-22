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

class SearchIndexService(using
  audioRepository: AudioRepository,
  audioIndexService: AudioIndexService
) extends StrictLogging {

