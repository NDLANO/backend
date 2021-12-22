/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import cats.implicits._
import no.ndla.audioapi.AudioApiProperties.{creatorTypeMap, processorTypeMap, rightsholderTypeMap}
import no.ndla.audioapi.integration.DraftApiClient
import no.ndla.audioapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain._
import no.ndla.language.model.Iso639
import no.ndla.mapping.License.getLicense
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.scalatra.servlet.FileItem

import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

trait ValidationService {
  this: DraftApiClient with ConverterService =>
  val validationService: ValidationService

  class ValidationService {

    def validatePodcastEpisodes(episodes: Seq[(Long, Option[AudioMetaInformation])],
                                seriesId: Option[Long]): Try[Seq[AudioMetaInformation]] = {
      val validated = episodes.map {
        case (id, Some(ep)) =>
          validatePodcastEpisode(id, ep, seriesId) match {
            case Nil  => Right(ep)
            case msgs => Left(msgs)
          }
        case (id, None) =>
          Left(
            Seq(
              ValidationMessage(
                s"episodes.$id",
                s"Provided episode with id '$id' was not found in the database."
              )))
      }

      val (errors, eps) = validated.separate
      val messages = errors.flatten

      validationTry(eps, messages)
    }

    private def validatePodcastEpisode(episodeId: Long,
                                       episode: AudioMetaInformation,
                                       seriesId: Option[Long]): Seq[ValidationMessage] = {
      val correctTypeError =
        if (episode.audioType != AudioType.Podcast)
          Seq(
            ValidationMessage(
              s"episodes.$episodeId",
              s"Provided episode $episodeId, is not of '${AudioType.Podcast}' type"
            ))
        else Seq.empty

      val overrideSeriesIdError = episode.seriesId match {
        case Some(episodeSeriesId) if !seriesId.contains(episodeSeriesId) =>
          Some(
            ValidationMessage(
              s"episodes.$episodeId",
              s"Provided episode $episodeId, is already a part of a series (With id: '$episodeSeriesId')."
            ))
        case _ => None
      }

      val hasPodcastMetaError = validateNonEmpty(s"episodes.$episodeId.podcastMeta", episode.podcastMeta)

      correctTypeError ++ overrideSeriesIdError ++ hasPodcastMetaError
    }

    def validateAudioFile(audioFile: FileItem): Option[ValidationMessage] = {
      val validMimeTypes = Seq("audio/mp3", "audio/mpeg")
      val actualMimeType = audioFile.getContentType.getOrElse("")

      if (!validMimeTypes.contains(actualMimeType)) {
        return Some(ValidationMessage(
          "files",
          s"The file ${audioFile.name} is not a valid audio file. Only valid types are '${validMimeTypes.mkString(",")}', but was '$actualMimeType'"))
      }

      if (audioFile.name.toLowerCase.endsWith(".mp3")) None
      else
        Some(
          ValidationMessage("files", s"The file ${audioFile.name} does not have a known file extension. Must be .mp3"))
    }

    def validate(audio: domain.AudioMetaInformation,
                 oldAudio: Option[domain.AudioMetaInformation],
                 partOfSeries: Option[domain.Series],
                 language: Option[String]): Try[domain.AudioMetaInformation] = {

      val oldTitleLanguages = oldAudio.map(_.titles.map(_.language)).getOrElse(Seq())
      val oldTagsLanguages = oldAudio.map(_.tags.map(_.language)).getOrElse(Seq())
      val oldLanguages = (oldTitleLanguages ++ oldTagsLanguages).distinct

      val validationMessages = validateNonEmpty("title", audio.titles).toSeq ++
        audio.titles.flatMap(title => validateNonEmpty("title", title.language)) ++
        audio.titles.flatMap(title => validateTitle("title", title, oldLanguages)) ++
        validateCopyright(audio.copyright) ++
        validateTags(audio.tags, oldLanguages) ++
        validatePodcastMeta(audio.audioType, audio.podcastMeta, language) ++
        validateEpisodeIfSeries(audio, partOfSeries)

      validationTry(audio, validationMessages)

    }

    private def validateEpisodeIfSeries(audio: AudioMetaInformation,
                                        series: Option[domain.Series]): Seq[ValidationMessage] = {
      if (audio.seriesId.isDefined) {
        val correctTypeError =
          if (audio.audioType != AudioType.Podcast)
            Some(
              ValidationMessage(
                s"seriesId",
                s"Audio must be of '${AudioType.Podcast}' type to add to series."
              ))
          else None

        val seriesExistsError =
          if (audio.seriesId.isDefined && series.isEmpty)
            Some(
              ValidationMessage(
                s"seriesId",
                s"Series specified did not exist."
              ))
          else None

        val hasPodcastMetaError = validateNonEmpty(s"podcastMeta", audio.podcastMeta)

        Seq(
          correctTypeError,
          seriesExistsError,
          hasPodcastMetaError
        ).flatten
      } else Seq.empty
    }

    def validate[T <: domain.SeriesWithoutId](series: T): Try[T] = {
      val validationMessages = validateNonEmpty("title", series.title).toSeq ++
        series.title.flatMap(title => validateNonEmpty("title", title.language)) ++
        series.title.flatMap(title => validateTitle("title", title, Seq.empty)) ++
        validateDescription(series.description) ++
        validatePodcastCoverPhoto("coverPhoto", series.coverPhoto)

      validationTry(series, validationMessages)
    }

    private[service] def readImage(imageUrl: String): BufferedImage = {
      val url = new URL(imageUrl)
      ImageIO.read(url)
    }

    def validatePodcastCoverPhoto(fieldName: String, coverPhoto: domain.CoverPhoto): Seq[ValidationMessage] = {
      val imageUrl = converterService.getPhotoUrl(coverPhoto)
      val image = readImage(imageUrl)
      val imageHeight = image.getHeight
      val imageWidth = image.getWidth

      val squareValidationMessage =
        if (imageHeight == imageWidth) Seq.empty
        else Seq(ValidationMessage(fieldName, "Podcast cover images must be exactly square to be valid."))

      val minImageSize = 1400
      val maxImageSize = 3000

      val isBigEnough = imageHeight >= minImageSize || imageWidth >= minImageSize
      val isSmallEnough = imageHeight <= maxImageSize || imageWidth <= maxImageSize

      val sizeValidationMessage =
        if (isBigEnough && isSmallEnough) Seq.empty
        else
          Seq(
            ValidationMessage(
              fieldName,
              s"Podcast cover images must be minimum $minImageSize and maximum $maxImageSize to be valid. The supplied image was ${imageWidth}x$imageHeight."
            ))

      squareValidationMessage ++ sizeValidationMessage
    }

    private def validateDescription(descriptions: Seq[Description]): Seq[ValidationMessage] = {
      descriptions.flatMap(validateDescription) ++ validateNonEmpty("description", descriptions)
    }

    private def validationTry[T](successCase: T, messages: Seq[ValidationMessage]): Try[T] = {
      messages match {
        case head :: tail => Failure(new ValidationException(errors = head :: tail))
        case _            => Success(successCase)
      }
    }

    def validatePodcastMeta(audioType: AudioType.Value,
                            meta: Seq[PodcastMeta],
                            language: Option[String]): Seq[ValidationMessage] = {
      if (meta.nonEmpty && audioType != AudioType.Podcast) {
        Seq(
          ValidationMessage("podcastMeta",
                            s"Cannot specify podcastMeta fields for audioType other than '${AudioType.Podcast}'"))
      } else {
        meta.flatMap(m => {
          val introductionErrors = validateNonEmpty("podcastMeta.introduction", m.introduction).toSeq
          val coverPhotoErrors = if (language.contains(m.language)) {
            validatePodcastCoverPhoto("podcastMeta.coverPhoto", m.coverPhoto)
          } else Seq.empty

          introductionErrors ++ coverPhotoErrors
        })
      }
    }

    private def validateTitle(fieldPath: String, title: Title, oldLanguages: Seq[String]): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, title.title).toList ++
        validateLanguage(fieldPath, title.language, oldLanguages)
    }

    private def validateDescription(desc: Description): Seq[ValidationMessage] = {
      containsNoHtml("description", desc.description).toList ++
        validateLanguage("description", desc.language, Seq.empty) ++
        validateMinimumLength("description", desc.description, 1)
    }

    private def validateMinimumLength(fieldPath: String, content: String, minLength: Int): Option[ValidationMessage] =
      if (content.trim.length < minLength)
        Some(
          ValidationMessage(fieldPath,
                            s"This field does not meet the minimum length requirement of $minLength characters"))
      else
        None

    def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      validateLicense(copyright.license).toList ++
        copyright.creators.flatMap(a => validateAuthor("copyright.creators", a, creatorTypeMap.values.toList)) ++
        copyright.processors.flatMap(a => validateAuthor("copyright.processors", a, processorTypeMap.values.toList)) ++
        copyright.rightsholders.flatMap(a =>
          validateAuthor("copyright.rightsholders", a, rightsholderTypeMap.values.toList)) ++
        validateAgreement(copyright) ++
        copyright.origin.flatMap(origin => containsNoHtml("copyright.origin", origin))
    }

    def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    def validateAgreement(copyright: Copyright): Seq[ValidationMessage] = {
      copyright.agreementId match {
        case Some(id) if !draftApiClient.agreementExists(id) =>
          Seq(ValidationMessage("copyright.agreement", s"Agreement with id $id does not exist"))
        case _ => Seq.empty
      }
    }

    def validateAuthor(fieldPath: String, author: Author, allowedTypes: Seq[String]): Seq[ValidationMessage] = {
      containsNoHtml(s"$fieldPath.type", author.`type`).toList ++
        containsNoHtml(s"$fieldPath.name", author.name).toList ++
        validateAuthorType(fieldPath, author.`type`, allowedTypes).toList
    }

    def validateAuthorType(fieldPath: String, `type`: String, allowedTypes: Seq[String]): Option[ValidationMessage] = {
      if (allowedTypes.contains(`type`.toLowerCase)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
    }

    def validateTags(tags: Seq[Tag], oldLanguages: Seq[String]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(containsNoHtml("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language, oldLanguages).toList
      })
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (Jsoup.isValid(text, Whitelist.none())) {
        None
      } else {
        Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed"))
      }
    }

    private def validateLanguage(fieldPath: String,
                                 languageCode: String,
                                 oldLanguages: Seq[String]): Option[ValidationMessage] = {
      if (languageCodeSupported639(languageCode) || oldLanguages.contains(languageCode)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def languageCodeSupported639(languageCode: String): Boolean = Iso639.get(languageCode).isSuccess

    private def validateNonEmpty(fieldPath: String, option: Option[_]): Option[ValidationMessage] = {
      option match {
        case Some(_) => None
        case None    => Some(ValidationMessage(fieldPath, "There is no element to validate."))
      }
    }

    def validateNonEmpty(fieldPath: String, sequence: Seq[Any]): Option[ValidationMessage] = {
      if (sequence.nonEmpty) {
        None
      } else {
        Some(ValidationMessage(fieldPath, "There are no elements to validate."))
      }
    }

  }
}
