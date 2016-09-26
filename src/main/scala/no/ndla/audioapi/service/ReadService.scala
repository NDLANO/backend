/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.api
import no.ndla.audioapi.repository.AudioRepository

trait ReadService {
  this: AudioRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withId(id: Long): Option[api.AudioMetaInformation] =
      audioRepository.withId(id).map(converterService.toApiAudioMetaInformation)

    def withExternalId(externalId: String): Option[api.AudioMetaInformation] =
      audioRepository.withExternalId(externalId).map(converterService.toApiAudioMetaInformation)

  }
}
