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
import no.ndla.audioapi.model.api.NotFoundException
import no.ndla.audioapi.model.domain.{Audio, Tag, Title}
import no.ndla.audioapi.model.Language._
import no.ndla.audioapi.model.{api, domain}
import no.ndla.mapping.License.getLicense

import scala.util.{Failure, Success, Try}


trait ConverterService {
  this: User with Clock =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toApiAudioMetaInformation(audioMeta: domain.AudioMetaInformation, language: String): Try[api.AudioMetaInformation] = {
      val lang = language match {
        case AllLanguages if audioMeta.supportedLanguages.contains(DefaultLanguage) => DefaultLanguage
        case AllLanguages if audioMeta.supportedLanguages.nonEmpty => audioMeta.supportedLanguages.head
        case l => l
      }

      if (!audioMeta.supportedLanguages.contains(lang))
        return Failure(new NotFoundException)

      val audioFile = findByLanguage(audioMeta.filePaths, lang).getOrElse(audioMeta.filePaths.head)
      Success(api.AudioMetaInformation(
        audioMeta.id.get,
        lang,
        findByLanguage(audioMeta.titles, lang).getOrElse(""),
        toApiAudio(audioFile),
        toApiCopyright(audioMeta.copyright),
        findByLanguage(audioMeta.tags, lang).getOrElse(Seq.empty),
        audioMeta.supportedLanguages
      ))
    }

    def toApiAudio(audio: domain.Audio): api.Audio = {
      val audioUrl: Uri = s"$Domain/$AudioFilesUrlSuffix/${audio.filePath}"
      api.Audio(audioUrl, audio.mimeType, audio.fileSize)
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

    def toDomainAudioMetaInformation(audio: api.NewAudioMetaInformation, filePath: Audio): domain.AudioMetaInformation = {
      domain.AudioMetaInformation(None,
        Seq(domain.Title(audio.title, Some(audio.language))),
        Seq(filePath),
        toDomainCopyright(audio.copyright),
        if (audio.tags.nonEmpty) Seq(domain.Tag(audio.tags, Some(audio.language))) else Seq(),
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
  }
}
