/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.learningpath.{Description, EmbedUrl, LearningStep}

import scala.util.{Failure, Success, Try}

trait LearningStepValidator {
  this: TitleValidator & LanguageValidator & TextValidator & UrlValidator =>
  val learningStepValidator: LearningStepValidator

  class LearningStepValidator {
    val noHtmlTextValidator            = new TextValidator(allowHtml = false)
    private val basicHtmlTextValidator = new TextValidator(allowHtml = true)
    private val urlValidator           = new UrlValidator()

    private val MISSING_DESCRIPTION_OR_EMBED_URL =
      "A learningstep is required to have either a description, embedUrl or both."

    def validate(newLearningStep: LearningStep, allowUnknownLanguage: Boolean = false): Try[LearningStep] = {
      validateLearningStep(newLearningStep, allowUnknownLanguage) match {
        case head :: tail => Failure(new ValidationException(errors = head :: tail))
        case _            => Success(newLearningStep)
      }
    }

    def validateLearningStep(newLearningStep: LearningStep, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      titleValidator.validate(newLearningStep.title, allowUnknownLanguage) ++
        validateDescription(newLearningStep.description, allowUnknownLanguage) ++
        validateEmbedUrl(newLearningStep.embedUrl, allowUnknownLanguage) ++
        validateLicense(newLearningStep.license).toList ++
        validateThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep).toList
    }

    def validateDescription(descriptions: Seq[Description], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      if (descriptions.isEmpty) {
        List()
      } else {
        descriptions.flatMap(description => {
          basicHtmlTextValidator
            .validate("description", description.description)
            .toList :::
            languageValidator
              .validate("language", description.language, allowUnknownLanguage)
              .toList
        })
      }
    }

    private def validateEmbedUrl(embedUrls: Seq[EmbedUrl], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      embedUrls.flatMap(embedUrl => {
        urlValidator.validate("embedUrl.url", embedUrl.url).toList :::
          languageValidator
            .validate("language", embedUrl.language, allowUnknownLanguage)
            .toList
      })
    }

    def validateLicense(licenseOpt: Option[String]): Option[ValidationMessage] = {
      licenseOpt match {
        case None => None
        case Some(license) =>
          noHtmlTextValidator.validate("license", license)
      }
    }

    private def validateThatDescriptionOrEmbedUrlOrBothIsDefined(
        newLearningStep: LearningStep
    ): Option[ValidationMessage] = {
      if (newLearningStep.description.isEmpty && newLearningStep.embedUrl.isEmpty) {
        Some(ValidationMessage("description|embedUrl", MISSING_DESCRIPTION_OR_EMBED_URL))
      } else {
        None
      }
    }
  }
}
