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
import no.ndla.audioapi.auth.User
import no.ndla.audioapi.model.domain.{Audio}
import no.ndla.audioapi.model.Language.{DefaultLanguage, NoLanguage, AllLanguages}
import no.ndla.audioapi.model.{api, domain}
import no.ndla.mapping.License.getLicense


trait ConverterService {
  this: User with Clock =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toApiAudioMetaInformation(audioMetaInformation: domain.AudioMetaInformation, language: String): api.AudioMetaInformation = {
      val title =
        if (language == AllLanguages)
          audioMetaInformation.titles
            .find(title => title.language.getOrElse(NoLanguage) == DefaultLanguage)
            .getOrElse(audioMetaInformation.titles.head.title).toString
        else
          audioMetaInformation.titles
            .filter(title => title.language.getOrElse(NoLanguage) == language)
            .map(title => if (title.language.getOrElse(NoLanguage) == NoLanguage) "" else title.title)
            .headOption
            .getOrElse("")

      api.AudioMetaInformation(
        audioMetaInformation.id.get,
        title,
        audioMetaInformation.filePaths.map(toApiAudio),
        toApiCopyright(audioMetaInformation.copyright),
        audioMetaInformation.tags.map(toApiTags)
      )
    }

    def toApiTitle(title: domain.Title): api.Title =
      api.Title(title.title, title.language)

    def toApiAudio(audio: domain.Audio): api.Audio = {
      val audioUrl: Uri = s"$Domain/$AudioFilesUrlSuffix/${audio.filePath}"
      api.Audio(audioUrl, audio.mimeType, audio.fileSize, audio.language)
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright =
      api.Copyright(toApiLicence(copyright.license), copyright.origin, copyright.authors.map(toApiAuthor))

    def toApiLicence(licenseAbbrevation: String): api.License = {
      getLicense(licenseAbbrevation) match {
        case Some(license) => api.License(license.license, Option(license.description), license.url)
        case None =>
          logger.warn("Could not retrieve license information for {}", licenseAbbrevation)
          api.License("unknown", None, None)
      }
    }

    def toApiAuthor(author: domain.Author): api.Author =
      api.Author(author.`type`, author.name)

    def toApiTags(tags: domain.Tag): api.Tag =
      api.Tag(tags.tags, tags.language)

    def toDomainAudioMetaInformation(audio: api.NewAudioMetaInformation, filePaths: Seq[Audio]): domain.AudioMetaInformation = {
      domain.AudioMetaInformation(None,
        audio.titles.map(toDomainTitle),
        filePaths,
        toDomainCopyright(audio.copyright),
        audio.tags.getOrElse(Seq()).map(toDomainTag),
        authUser.id(),
        clock.now()
      )
    }

    def toDomainTitle(title: api.Title): domain.Title = {
      domain.Title(title.title, title.language)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(copyright.license.license, copyright.origin, copyright.authors.map(toDomainAuthor))
    }

    def toDomainAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def toDomainTag(tag: api.Tag): domain.Tag = {
      domain.Tag(tag.tags, tag.language)
    }

  }
}
