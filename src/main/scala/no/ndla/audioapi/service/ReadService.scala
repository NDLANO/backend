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
    def withId(id: Long, language: String): Option[api.AudioMetaInformation] =
      audioRepository.withId(id).map(audio => converterService.toApiAudioMetaInformation(audio, language))

    def withExternalId(externalId: String, language: String): Option[api.AudioMetaInformation] =
      audioRepository.withExternalId(externalId).map(audio => converterService.toApiAudioMetaInformation(audio, language))

  }
}
