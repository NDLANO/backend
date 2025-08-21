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
import no.ndla.common.model.domain.RevisionStatus
import no.ndla.common.model.NDLADate

import scala.util.{Failure, Success, Try}

trait LearningPathValidator {
  this: LanguageValidator & TitleValidator & TextValidator =>
  lazy val learningPathValidator: LearningPathValidator

  class LearningPathValidator(descriptionRequired: Boolean = false) {

    private val MY_NDLA_LANGUAGE_MISMATCH =
      "A learning path created in MyNDLA must have exactly one supported language."

    private val MY_NDLA_INVALID_LANGUAGES =
      "A learning path created in MyNDLA must have exactly one supported language."

    private val MISSING_DESCRIPTION = "At least one description is required."

    private val INVALID_COVER_PHOTO =
      "The url to the coverPhoto must point to an image in NDLA Image API."

    val noHtmlTextValidator       = new TextValidator(allowHtml = false)
    private val durationValidator = new DurationValidator

    def validate(newLearningPath: LearningPath, allowUnknownLanguage: Boolean = false): Try[LearningPath] = {
      validateLearningPath(newLearningPath, allowUnknownLanguage) match {
        case head :: tail =>
          Failure(ValidationException(errors = head :: tail))
        case _ => Success(newLearningPath)
      }
    }

    def validate(
        updatedLearningPath: UpdatedLearningPathV2DTO,
        existing: LearningPath
    ): Try[UpdatedLearningPathV2DTO] = {
      validateLearningPathUpdate(updatedLearningPath, existing) match {
        case head :: tail => Failure(ValidationException(errors = head :: tail))
        case _            => Success(updatedLearningPath)
      }
    }

    private[validation] def validateLearningPath(
        newLearningPath: LearningPath,
        allowUnknownLanguage: Boolean
    ): Seq[ValidationMessage] = {
      validateSupportedLanguages(newLearningPath) ++
        titleValidator.validate(newLearningPath.title, allowUnknownLanguage) ++
        validateDescription(newLearningPath.description, allowUnknownLanguage) ++
        validateDuration(newLearningPath.duration).toList ++
        validateTags(newLearningPath.tags, allowUnknownLanguage) ++
        validateCopyright(newLearningPath.copyright) ++
        validateRevisionMeta(newLearningPath)
    }

    private[validation] def validateLearningPathUpdate(
        updatedLearningPath: UpdatedLearningPathV2DTO,
        existing: LearningPath
    ): Seq[ValidationMessage] =
      validateUpdateLanguage(updatedLearningPath, existing) ++
        languageValidator.validate(
          "language",
          updatedLearningPath.language,
          allowUnknownLanguage = true
        )

    private def validateSupportedLanguages(learningPath: LearningPath) =
      (learningPath.supportedLanguages.size, learningPath.isMyNDLAOwner) match {
        case (1, true)  => List()
        case (_, true)  => List(ValidationMessage("supportedLanguages", MY_NDLA_INVALID_LANGUAGES))
        case (_, false) => List()
      }

    private def validateDescription(
        descriptions: Seq[Description],
        allowUnknownLanguage: Boolean
    ): Seq[ValidationMessage] = {
      (descriptionRequired, descriptions.isEmpty) match {
        case (false, true) => List()
        case (true, true)  =>
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
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      noHtmlTextValidator.validate("author.name", author.name).toList
    }

    private def validateRevisionMeta(lp: LearningPath): Seq[ValidationMessage] = lp.isMyNDLAOwner match {
      case true  => Seq.empty
      case false =>
        lp.revisionMeta.find(rm =>
          rm.status == RevisionStatus.NeedsRevision && rm.revisionDate.isAfter(NDLADate.now())
        ) match {
          case None =>
            Seq(ValidationMessage("revisionMeta", "An article must contain at least one planned revision date"))
          case _ => Seq.empty
        }
    }

    private def validateUpdateLanguage(
        updatedLearningPath: UpdatedLearningPathV2DTO,
        existing: LearningPath
    ): Seq[ValidationMessage] = {
      if (existing.isMyNDLAOwner && updatedLearningPath.language != existing.supportedLanguages.head) {
        Seq(ValidationMessage("language", MY_NDLA_LANGUAGE_MISMATCH))
      } else {
        Seq.empty
      }
    }
  }
}
