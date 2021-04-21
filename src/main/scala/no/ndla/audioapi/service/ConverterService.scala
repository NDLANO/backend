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
import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType, PodcastMeta}
import no.ndla.audioapi.model.{Language, api, domain}
import no.ndla.mapping.License.getLicense

import scala.util.{Success, Try}

trait ConverterService {
  this: User with Clock with DraftApiClient =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {

    def withoutLanguage(audio: AudioMetaInformation, language: String) =
      audio.copy(
        titles = audio.titles.filterNot(_.language == language),
        filePaths = audio.filePaths.filterNot(_.language == language),
        tags = audio.tags.filterNot(_.language == language)
      )

    def withAgreementCopyright(audio: AudioMetaInformation): AudioMetaInformation = {
      val agreementCopyright = audio.copyright.agreementId
        .flatMap(aid => draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright))
        .getOrElse(audio.copyright)

      audio.copy(
        copyright = audio.copyright.copy(
          license = agreementCopyright.license,
          creators = agreementCopyright.creators,
          rightsholders = agreementCopyright.rightsholders,
          validFrom = agreementCopyright.validFrom,
          validTo = agreementCopyright.validTo
        ))
    }

    def withAgreementCopyright(copyright: api.Copyright): api.Copyright = {
      val agreementCopyright =
        copyright.agreementId.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(copyright)
      copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      )
    }

    def toApiAudioMetaInformation(audioMeta: domain.AudioMetaInformation,
                                  language: Option[String]): Try[api.AudioMetaInformation] = {
      Success(
        api.AudioMetaInformation(
          id = audioMeta.id.get,
          revision = audioMeta.revision.get,
          title = toApiTitle(findByLanguageOrBestEffort(audioMeta.titles, language)),
          audioFile = toApiAudio(findByLanguageOrBestEffort(audioMeta.filePaths, language)),
          copyright = withAgreementCopyright(toApiCopyright(audioMeta.copyright)),
          tags = toApiTags(findByLanguageOrBestEffort(audioMeta.tags, language)),
          supportedLanguages = audioMeta.supportedLanguages,
          audioType = audioMeta.audioType.toString,
          podcastMeta = findByLanguageOrBestEffort(audioMeta.podcastMeta, language).map(toApiPodcastMeta),
          manuscript = findByLanguageOrBestEffort(audioMeta.manuscript, language).map(toApiManuscript)
        ))
    }

    def toApiTitle(maybeTitle: Option[domain.Title]): api.Title = {
      maybeTitle match {
        case Some(title) => api.Title(title.title, title.language)
        case None        => api.Title("", DefaultLanguage)
      }
    }

    def toApiTags(maybeTag: Option[domain.Tag]) = {
      maybeTag match {
        case Some(tag) => api.Tag(tag.tags, tag.language)
        case None      => api.Tag(Seq(), DefaultLanguage)
      }
    }

    def toApiAudio(audio: Option[domain.Audio]): api.Audio = {
      audio match {
        case Some(x) => api.Audio(s"$Domain/$AudioFilesUrlSuffix/${x.filePath}", x.mimeType, x.fileSize, x.language)
        case None    => api.Audio("", "", 0, DefaultLanguage)
      }
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright =
      withAgreementCopyright(
        api.Copyright(
          toApiLicence(copyright.license),
          copyright.origin,
          copyright.creators.map(toApiAuthor),
          copyright.processors.map(toApiAuthor),
          copyright.rightsholders.map(toApiAuthor),
          copyright.agreementId,
          copyright.validFrom,
          copyright.validTo
        )
      )

    def toApiLicence(licenseAbbrevation: String): api.License = {
      getLicense(licenseAbbrevation) match {
        case Some(license) => api.License(license.license.toString, Option(license.description), license.url)
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
        case true  => Seq()
      }
    }

    private def toApiPodcastMeta(meta: domain.PodcastMeta): api.PodcastMeta = {
      api.PodcastMeta(
        header = meta.header,
        introduction = meta.introduction,
        coverPhoto = toApiCoverPhoto(meta.coverPhoto),
        language = meta.language
      )
    }

    private def toApiManuscript(meta: domain.Manuscript): api.Manuscript = {
      api.Manuscript(
        manuscript = meta.manuscript,
        language = meta.language
      )
    }
    private def toApiCoverPhoto(meta: domain.CoverPhoto): api.CoverPhoto = {
      api.CoverPhoto(
        id = meta.imageId,
        url = s"$RawImageApiUrl/${meta.imageId}",
        altText = meta.altText
      )
    }

    def toDomainPodcastMeta(meta: api.NewPodcastMeta, language: String): PodcastMeta = {
      domain.PodcastMeta(
        header = meta.header,
        introduction = meta.introduction,
        coverPhoto = domain.CoverPhoto(meta.coverPhotoId, meta.coverPhotoAltText),
        language = language
      )
    }

    def toDomainManuscript(manuscript: String, language: String): domain.Manuscript = {
      domain.Manuscript(manuscript = manuscript, language = language)
    }

    def toDomainAudioMetaInformation(audioMeta: api.NewAudioMetaInformation,
                                     audio: domain.Audio): domain.AudioMetaInformation = {
      domain.AudioMetaInformation(
        id = None,
        revision = None,
        titles = Seq(domain.Title(audioMeta.title, audioMeta.language)),
        filePaths = Seq(audio),
        copyright = toDomainCopyright(audioMeta.copyright),
        tags = if (audioMeta.tags.nonEmpty) Seq(domain.Tag(audioMeta.tags, audioMeta.language)) else Seq(),
        updatedBy = authUser.userOrClientid(),
        updated = clock.now(),
        podcastMeta = audioMeta.podcastMeta.map(m => toDomainPodcastMeta(m, audioMeta.language)).toSeq,
        audioType = audioMeta.audioType.flatMap(AudioType.valueOf).getOrElse(AudioType.Standard),
        manuscript = audioMeta.manuscript.map(m => toDomainManuscript(m, audioMeta.language)).toSeq
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
