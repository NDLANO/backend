/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.{domain, api}

trait ConverterServiceComponent {
  val converterService: ConverterService

  class ConverterService {
    def toApiAudioMetaInformation(audioMetaInformation: domain.AudioMetaInformation): api.AudioMetaInformation = {
      api.AudioMetaInformation(audioMetaInformation.id.get,
        audioMetaInformation.titles.map(toApiTitle),
        audioMetaInformation.filePaths.map(toApiAudio),
        toApiCopyright(audioMetaInformation.copyright))
    }

    def toApiTitle(title: domain.Title): api.Title =
      api.Title(title.title, title.language)

    def toApiAudio(audio: domain.Audio): api.Audio =
      api.Audio(audio.filePath, audio.mimeType, audio.fileSize, audio.language)

    def toApiCopyright(copyright: domain.Copyright): api.Copyright =
      api.Copyright(copyright.license, copyright.origin, copyright.authors.map(toApiAuthor))

    def toApiAuthor(author: domain.Author): api.Author =
      api.Author(author.`type`, author.name)

  }
}
