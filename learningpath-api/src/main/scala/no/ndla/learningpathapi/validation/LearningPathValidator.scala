/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import io.lemonlabs.uri.Url
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.{Author, Tag}
import no.ndla.common.model.domain.learningpath.{Description, LearningPath, LearningpathCopyright}
import no.ndla.learningpathapi.model.api.UpdatedLearningPathV2DTO
import no.ndla.mapping.License.getLicense

trait LearningPathValidator {
  this: LanguageValidator & TitleValidator & TextValidator =>
  val learningPathValidator: LearningPathValidator

  class LearningPathValidator(descriptionRequired: Boolean = false) {

    private val MISSING_DESCRIPTION = "At least one description is required."

    private val INVALID_COVER_PHOTO =
      "The url to the coverPhoto must point to an image in NDLA Image API."

    val noHtmlTextValidator       = new TextValidator(allowHtml = false)
    private val durationValidator = new DurationValidator

    def validate(newLearningPath: LearningPath, allowUnknownLanguage: Boolean = false): Unit = {
      validateLearningPath(newLearningPath, allowUnknownLanguage) match {
        case head :: tail =>
          throw new ValidationException(errors = head :: tail)
        case _ =>
      }
    }

    def validate(updateLearningPath: UpdatedLearningPathV2DTO): Unit = {
      languageValidator.validate("language", updateLearningPath.language, allowUnknownLanguage = true) match {
        case None =>
        case Some(validationMessage) =>
          throw new ValidationException(errors = Seq(validationMessage))
      }
    }

    private[validation] def validateLearningPath(
        newLearningPath: LearningPath,
        allowUnknownLanguage: Boolean
    ): Seq[ValidationMessage] = {
      titleValidator.validate(newLearningPath.title, allowUnknownLanguage) ++
        validateDescription(newLearningPath.description, allowUnknownLanguage) ++
        validateDuration(newLearningPath.duration).toList ++
        validateTags(newLearningPath.tags, allowUnknownLanguage) ++
        validateCopyright(newLearningPath.copyright)
    }

    private def validateDescription(
        descriptions: Seq[Description],
        allowUnknownLanguage: Boolean
    ): Seq[ValidationMessage] = {
      (descriptionRequired, descriptions.isEmpty) match {
        case (false, true) => List()
        case (true, true) =>
          List(ValidationMessage("description", MISSING_DESCRIPTION))
        case (_, false) =>
          descriptions.flatMap(description => {
            noHtmlTextValidator
              .validate("description.description", description.description)
              .toList :::
              languageValidator
                .validate("description.language", description.language, allowUnknownLanguage)
                .toList
          })
      }
    }

    private def validateDuration(durationOpt: Option[Int]): Option[ValidationMessage] = {
      durationOpt match {
        case None    => None
        case Some(_) => durationValidator.validateRequired(durationOpt)
      }
    }

    def validateCoverPhoto(coverPhotoMetaUrl: String): Option[ValidationMessage] = {
      val parsedUrl = Url.parse(coverPhotoMetaUrl)
      val host      = parsedUrl.hostOption.map(_.toString)

      val hostCorrect = host.getOrElse("").endsWith("ndla.no")
      val pathCorrect = parsedUrl.path.toString.startsWith("/image-api/v")

      if (hostCorrect && pathCorrect) {
        None
      } else {
        Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO))
      }
    }

    private def validateTags(tags: Seq[Tag], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags
          .flatMap(noHtmlTextValidator.validate("tags.tags", _))
          .toList :::
          languageValidator
            .validate("tags.language", tagList.language, allowUnknownLanguage)
            .toList
      })
    }

    private def validateCopyright(copyright: LearningpathCopyright): Seq[ValidationMessage] = {
      val licenseMessage       = validateLicense(copyright.license)
      val contributorsMessages = copyright.contributors.flatMap(validateAuthor)

      licenseMessage ++ contributorsMessages
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None =>
          Seq(new ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      noHtmlTextValidator.validate("author.type", author.`type`).toList ++
        noHtmlTextValidator.validate("author.name", author.name).toList
    }
  }

}
