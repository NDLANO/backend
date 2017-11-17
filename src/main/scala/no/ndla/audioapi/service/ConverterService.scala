/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties._
import no.ndla.audioapi.auth.User
import no.ndla.audioapi.integration.DraftApiClient
import no.ndla.audioapi.model.Language.{DefaultLanguage, findByLanguageOrBestEffort}
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.{api, domain}
import no.ndla.mapping.License.getLicense

import scala.util.{Success, Try}


trait ConverterService {
  this: User with Clock with DraftApiClient =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def withAgreementCopyright(audio: AudioMetaInformation): AudioMetaInformation = {
      val agreementCopyright = audio.copyright.agreementId.flatMap(aid =>
        draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright)
      ).getOrElse(audio.copyright)

      audio.copy(copyright = audio.copyright.copy(
        license = agreementCopyright.license,
        creators = if (agreementCopyright.creators.nonEmpty) agreementCopyright.creators else audio.copyright.creators,
        rightsholders = if (agreementCopyright.rightsholders.nonEmpty) agreementCopyright.rightsholders else audio.copyright.rightsholders,
        validFrom = if (agreementCopyright.validFrom.nonEmpty) agreementCopyright.validFrom else audio.copyright.validFrom,
        validTo = if (agreementCopyright.validTo.nonEmpty) agreementCopyright.validTo else audio.copyright.validTo
      ))
    }

    def withAgreementCopyright(audio: api.AudioMetaInformation): api.AudioMetaInformation = {
      val agreementCopyright = audio.copyright.agreementId.flatMap(aid =>
        draftApiClient.getAgreementCopyright(aid)
      ).getOrElse(audio.copyright)

      audio.copy(copyright = audio.copyright.copy(
        license = agreementCopyright.license,
        creators = if (agreementCopyright.creators.nonEmpty) agreementCopyright.creators else audio.copyright.creators,
        rightsholders = if (agreementCopyright.rightsholders.nonEmpty) agreementCopyright.rightsholders else audio.copyright.rightsholders,
        validFrom = if (agreementCopyright.validFrom.nonEmpty) agreementCopyright.validFrom else audio.copyright.validFrom,
        validTo = if (agreementCopyright.validTo.nonEmpty) agreementCopyright.validTo else audio.copyright.validTo
      ))
    }

    def toApiAudioMetaInformation(audioMeta: domain.AudioMetaInformation, language: Option[String]): Try[api.AudioMetaInformation] = {
      Success(api.AudioMetaInformation(
        audioMeta.id.get,
        audioMeta.revision.get,
        toApiTitle(findByLanguageOrBestEffort(audioMeta.titles, language)),
        toApiAudio(findByLanguageOrBestEffort(audioMeta.filePaths, language)),
        toApiCopyright(audioMeta.copyright),
        toApiTags(findByLanguageOrBestEffort(audioMeta.tags, language)),
        audioMeta.supportedLanguages
      ))
    }

    def toApiTitle(maybeTitle: Option[domain.Title]): api.Title = {
      maybeTitle match {
        case Some(title) => api.Title(title.title, title.language)
        case None => api.Title("", DefaultLanguage)
      }
    }

    def toApiTags(maybeTag: Option[domain.Tag]) = {
      maybeTag match {
        case Some(tag) => api.Tag(tag.tags, tag.language)
        case None => api.Tag(Seq(), DefaultLanguage)
      }
    }

    def toApiAudio(audio: Option[domain.Audio]): api.Audio = {
      audio match {
        case Some(x) => api.Audio(s"$Domain/$AudioFilesUrlSuffix/${x.filePath}", x.mimeType, x.fileSize, x.language)
        case None => api.Audio("", "", 0, DefaultLanguage)
      }
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright =
      api.Copyright(toApiLicence(copyright.license),
        copyright.origin,
        copyright.creators.map(toApiAuthor),
        copyright.processors.map(toApiAuthor),
        copyright.rightsholders.map(toApiAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )

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

    def toDomainTags(tags: api.Tag): Seq[domain.Tag] = {
      tags.tags.nonEmpty match {
        case false => Seq(domain.Tag(tags.tags, tags.language))
        case true => Seq()
      }
    }

    def toDomainAudioMetaInformation(audioMeta: api.NewAudioMetaInformation, audio: domain.Audio): domain.AudioMetaInformation = {
      domain.AudioMetaInformation(None, None,
        Seq(domain.Title(audioMeta.title, audioMeta.language)),
        Seq(audio),
        toDomainCopyright(audioMeta.copyright),
        if (audioMeta.tags.nonEmpty) Seq(domain.Tag(audioMeta.tags, audioMeta.language)) else Seq(),
        authUser.id(),
        clock.now()
      )
    }

    def toDomainTitle(title: api.Title): domain.Title = {
      domain.Title(title.title, title.language)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toDomainAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }
  }
}
