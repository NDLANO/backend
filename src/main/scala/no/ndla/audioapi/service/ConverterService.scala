/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import com.netaporter.uri.Uri
import com.typesafe.scalalogging.LazyLogging
import com.netaporter.uri.dsl._
import no.ndla.audioapi.AudioApiProperties._
import no.ndla.audioapi.model.{api, domain}
import no.ndla.mapping.License.getLicense

trait ConverterService {
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toApiAudioMetaInformation(audioMetaInformation: domain.AudioMetaInformation): api.AudioMetaInformation = {
      api.AudioMetaInformation(audioMetaInformation.id.get,
        audioMetaInformation.titles.map(toApiTitle),
        audioMetaInformation.filePaths.map(toApiAudio),
        toApiCopyright(audioMetaInformation.copyright),
        audioMetaInformation.tags.map(toApiTags))
    }

    def toApiTitle(title: domain.Title): api.Title =
      api.Title(title.title, title.language)

    def toApiAudio(audio: domain.Audio): api.Audio = {
      val audioUrl: Uri = s"http://$Domain/$AudioFilesUrlSuffix/${audio.filePath}"
      api.Audio(audioUrl, audio.mimeType, audio.fileSize, audio.language)
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright =
      api.Copyright(toApiLicence(copyright.license), copyright.origin, copyright.authors.map(toApiAuthor))

    def toApiLicence(licenseAbbrevation: String): api.License = {
      getLicense(licenseAbbrevation) match {
        case Some(license) => api.License(license.license, license.description, license.url)
        case None =>
          logger.warn("Could not retrieve license information for {}", licenseAbbrevation)
          api.License("unknown", "", None)
      }
    }

    def toApiAuthor(author: domain.Author): api.Author =
      api.Author(author.`type`, author.name)

    def toApiTags(tags: domain.Tag): api.Tag =
      api.Tag(tags.tags, tags.language)
  }
}
