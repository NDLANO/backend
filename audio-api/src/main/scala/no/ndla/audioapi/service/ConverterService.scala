/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.{CouldNotFindLanguageException, TagDTO}
import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType, PodcastMeta}
import no.ndla.audioapi.model.{api, domain}
import no.ndla.common.Clock
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.{NDLADate, api => commonApi, domain => common}
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.language.model.WithLanguage
import no.ndla.mapping.License.getLicense
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with Props =>
  val converterService: ConverterService

  class ConverterService extends StrictLogging {
    import props._

    def updateSeries(existingSeries: domain.Series, updatedSeries: api.NewSeriesDTO): domain.Series = {
      val newTitle       = common.Title(updatedSeries.title, updatedSeries.language)
      val newDescription = domain.Description(updatedSeries.description, updatedSeries.language)
      val coverPhoto = domain.CoverPhoto(
        imageId = updatedSeries.coverPhotoId,
        altText = updatedSeries.coverPhotoAltText
      )

      domain.Series(
        id = existingSeries.id,
        revision = updatedSeries.revision.getOrElse(0),
        episodes = None,
        title = mergeLanguageField(existingSeries.title, newTitle),
        description = mergeLanguageField(existingSeries.description, newDescription),
        coverPhoto = coverPhoto,
        updated = NDLADate.now(),
        created = existingSeries.created,
        hasRSS = updatedSeries.hasRSS.getOrElse(existingSeries.hasRSS)
      )
    }

    def toDomainSeries(newSeries: api.NewSeriesDTO): domain.SeriesWithoutId = {
      val titles       = Seq(common.Title(newSeries.title, newSeries.language))
      val descriptions = Seq(domain.Description(newSeries.description, newSeries.language))

      val coverPhoto = domain.CoverPhoto(
        imageId = newSeries.coverPhotoId,
        altText = newSeries.coverPhotoAltText
      )

      val createdDate = NDLADate.now()

      new domain.SeriesWithoutId(
        title = titles,
        description = descriptions,
        coverPhoto = coverPhoto,
        episodes = None,
        updated = createdDate,
        created = createdDate,
        hasRSS = newSeries.hasRSS.getOrElse(false)
      )
    }

    def withoutLanguage(audio: AudioMetaInformation, language: String): AudioMetaInformation =
      audio.copy(
        titles = audio.titles.filterNot(_.language == language),
        filePaths = audio.filePaths.filterNot(_.language == language),
        tags = audio.tags.filterNot(_.language == language),
        manuscript = audio.manuscript.filterNot(_.language == language),
        podcastMeta = audio.podcastMeta.filterNot(_.language == language)
      )

    def withoutLanguage(series: domain.Series, language: String): domain.Series = {
      domain.Series(
        id = series.id,
        revision = series.revision,
        episodes = series.episodes,
        title = series.title.filterNot(_.language == language),
        description = series.description.filterNot(_.language == language),
        coverPhoto = series.coverPhoto,
        updated = NDLADate.now(),
        created = series.created,
        hasRSS = false
      )
    }

    def toApiAudioMetaInformation(
        audioMeta: domain.AudioMetaInformation,
        language: Option[String]
    ): Try[api.AudioMetaInformationDTO] = {

      val apiSeries = audioMeta.series.traverse(series => toApiSeries(series, language))

      apiSeries.map(series =>
        api.AudioMetaInformationDTO(
          id = audioMeta.id.get,
          revision = audioMeta.revision.get,
          title = maybeToApiTitle(findByLanguageOrBestEffort(audioMeta.titles, language)),
          audioFile = toApiAudio(findByLanguageOrBestEffort(audioMeta.filePaths, language)),
          copyright = toApiCopyright(audioMeta.copyright),
          tags = toApiTags(findByLanguageOrBestEffort(audioMeta.tags, language)),
          supportedLanguages = audioMeta.supportedLanguages,
          audioType = audioMeta.audioType.toString,
          podcastMeta = findByLanguageOrBestEffort(audioMeta.podcastMeta, language).map(toApiPodcastMeta),
          series = series,
          manuscript = findByLanguageOrBestEffort(audioMeta.manuscript, language).map(toApiManuscript),
          created = audioMeta.created,
          updated = audioMeta.updated
        )
      )
    }

    private def toApiTitle(title: common.Title): api.TitleDTO = api.TitleDTO(title.title, title.language)
    private def toApiDescription(desc: domain.Description): api.DescriptionDTO =
      api.DescriptionDTO(desc.description, desc.language)

    private def maybeToApiTitle(maybeTitle: Option[common.Title]): api.TitleDTO = {
      maybeTitle match {
        case Some(title) => toApiTitle(title)
        case None        => api.TitleDTO("", DefaultLanguage)
      }
    }

    private def toApiTags(maybeTag: Option[common.Tag]): TagDTO = {
      maybeTag match {
        case Some(tag) => api.TagDTO(tag.tags, tag.language)
        case None      => api.TagDTO(Seq(), DefaultLanguage)
      }
    }

    def toApiAudio(audio: Option[domain.Audio]): api.AudioDTO = {
      audio match {
        case Some(x) => api.AudioDTO(s"$Domain/$AudioFilesUrlSuffix/${x.filePath}", x.mimeType, x.fileSize, x.language)
        case None    => api.AudioDTO("", "", 0, DefaultLanguage)
      }
    }

    def toApiLicence(licenseAbbrevation: String): commonApi.LicenseDTO = {
      getLicense(licenseAbbrevation) match {
        case Some(license) => commonApi.LicenseDTO(license.license.toString, Option(license.description), license.url)
        case None =>
          logger.warn("Could not retrieve license information for {}", licenseAbbrevation)
          commonApi.LicenseDTO("unknown", None, None)
      }
    }

    def toApiCopyright(copyright: Copyright): commonApi.CopyrightDTO = {
      commonApi.CopyrightDTO(
        toApiLicence(copyright.license),
        copyright.origin,
        copyright.creators.map(_.toApi),
        copyright.processors.map(_.toApi),
        copyright.rightsholders.map(_.toApi),
        copyright.validFrom,
        copyright.validTo,
        copyright.processed
      )
    }

    def toDomainTags(tags: api.TagDTO): Seq[common.Tag] = {
      if (tags.tags.nonEmpty) { Seq() }
      else { Seq(common.Tag(tags.tags, tags.language)) }
    }

    def toApiPodcastMeta(meta: domain.PodcastMeta): api.PodcastMetaDTO = {
      api.PodcastMetaDTO(
        introduction = meta.introduction,
        coverPhoto = toApiCoverPhoto(meta.coverPhoto),
        language = meta.language
      )
    }

    def toApiManuscript(meta: domain.Manuscript): api.ManuscriptDTO = {
      api.ManuscriptDTO(
        manuscript = meta.manuscript,
        language = meta.language
      )
    }

    def getPhotoUrl(meta: domain.CoverPhoto): String = s"$RawImageApiUrl/${meta.imageId}"

    def toApiCoverPhoto(meta: domain.CoverPhoto): api.CoverPhotoDTO = {
      api.CoverPhotoDTO(
        id = meta.imageId,
        url = getPhotoUrl(meta),
        altText = meta.altText
      )
    }

    def toDomainPodcastMeta(meta: api.NewPodcastMetaDTO, language: String): PodcastMeta = {
      domain.PodcastMeta(
        introduction = meta.introduction,
        coverPhoto = domain.CoverPhoto(meta.coverPhotoId, meta.coverPhotoAltText),
        language = language
      )
    }

    def toDomainManuscript(manuscript: String, language: String): domain.Manuscript = {
      domain.Manuscript(manuscript = manuscript, language = language)
    }

    def toDomainAudioMetaInformation(
        audioMeta: api.NewAudioMetaInformationDTO,
        audio: domain.Audio,
        maybeSeries: Option[domain.Series],
        tokenUser: TokenUser
    ): domain.AudioMetaInformation = {
      domain.AudioMetaInformation(
        id = None,
        revision = None,
        titles = Seq(common.Title(audioMeta.title, audioMeta.language)),
        filePaths = Seq(audio),
        copyright = toDomainCopyright(audioMeta.copyright),
        tags = if (audioMeta.tags.nonEmpty) Seq(common.Tag(audioMeta.tags, audioMeta.language)) else Seq(),
        updatedBy = tokenUser.id,
        updated = clock.now(),
        created = clock.now(),
        podcastMeta = audioMeta.podcastMeta.map(m => toDomainPodcastMeta(m, audioMeta.language)).toSeq,
        audioType = audioMeta.audioType.flatMap(AudioType.valueOf).getOrElse(AudioType.Standard),
        manuscript = audioMeta.manuscript.map(m => toDomainManuscript(m, audioMeta.language)).toSeq,
        series = maybeSeries.map(_.copy(episodes = None)),
        seriesId = audioMeta.seriesId
      )
    }

    def toDomainCopyright(copyright: commonApi.CopyrightDTO): Copyright = {
      Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(_.toDomain),
        copyright.processors.map(_.toDomain),
        copyright.rightsholders.map(_.toDomain),
        copyright.validFrom,
        copyright.validTo,
        copyright.processed
      )
    }

    def findAndConvertDomainToApiField[DomainType <: WithLanguage](
        fields: Seq[DomainType],
        language: Option[String]
    )(implicit mf: Manifest[DomainType]): Try[DomainType] = {
      findByLanguageOrBestEffort(fields, language.getOrElse(DefaultLanguage)) match {
        case Some(field) => Success(field)
        case None =>
          Failure(
            CouldNotFindLanguageException(
              s"Could not find value for '${mf.runtimeClass.getName}' field. This is a data inconsistency or a bug."
            )
          )
      }
    }

    def toApiSeries(series: domain.Series, language: Option[String]): Try[api.SeriesDTO] = {
      for {
        title       <- findAndConvertDomainToApiField(series.title, language).map(toApiTitle)
        description <- findAndConvertDomainToApiField(series.description, language).map(toApiDescription)
        coverPhoto = toApiCoverPhoto(series.coverPhoto)
        episodes <- series.episodes.traverse(eps => eps.traverse(toApiAudioMetaInformation(_, language)))
      } yield api.SeriesDTO(
        id = series.id,
        revision = series.revision,
        title = title,
        description = description,
        coverPhoto = coverPhoto,
        episodes = episodes,
        supportedLanguages = series.supportedLanguages,
        hasRSS = series.hasRSS
      )
    }

    def mergeLanguageField[T <: WithLanguage](field: Seq[T], toAdd: Option[T], language: String): Seq[T] = {
      field.indexWhere(_.language == language) match {
        case idx if idx >= 0 => field.patch(idx, toAdd.toSeq, 1)
        case _               => field ++ toAdd.toSeq
      }
    }

    def mergeLanguageField[Y <: WithLanguage](field: Seq[Y], toMerge: Y): Seq[Y] = {
      field.indexWhere(_.language == toMerge.language) match {
        case idx if idx >= 0 => field.patch(idx, Seq(toMerge), 1)
        case _               => field ++ Seq(toMerge)
      }
    }
  }

}
