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

    def withId(id: Long, language: Option[String]): Option[api.AudioMetaInformation] =
      audioRepository.withId(id).flatMap(audio => converterService.toApiAudioMetaInformation(audio, language).toOption)

    def withExternalId(externalId: String, language: Option[String]): Option[api.AudioMetaInformation] =
      audioRepository
        .withExternalId(externalId)
        .flatMap(audio => converterService.toApiAudioMetaInformation(audio, language).toOption)

    def getMetaAudioDomainDump(pageNo: Int, pageSize: Int): api.AudioMetaDomainDump = {
      val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
      val results = audioRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.AudioMetaDomainDump(audioRepository.audioCount, pageNo, pageSize, results)
    }
  }
}
