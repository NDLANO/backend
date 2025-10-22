/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.learningpath.{Description, EmbedUrl, Introduction, LearningPath, LearningStep}
import no.ndla.learningpathapi.Props

import scala.util.{Failure, Success, Try}

class LearningStepValidator(using props: Props, languageValidator: LanguageValidator, titleValidator: TitleValidator) {
  val noHtmlTextValidator              = TextValidator(allowHtml = false)
  private val allowedHtmlTextValidator = TextValidator(allowHtml = true)
  private val urlValidator             = new UrlValidator()

  private val MY_NDLA_INVALID_LANGUAGES = "A learning step created in MyNDLA must have exactly one supported language."

  private val MISSING_DESCRIPTION_OR_EMBED_URL_OR_ARTICLE_ID =
    "A learningstep is required to have either a description, an embedUrl, or an articleId."

  def validate(
      newLearningStep: LearningStep,
      learningPath: LearningPath,
      allowUnknownLanguage: Boolean = false,
  ): Try[LearningStep] = {
    validateLearningStep(newLearningStep, learningPath, allowUnknownLanguage) match {
      case head :: tail => Failure(new ValidationException(errors = head :: tail))
      case _            => Success(newLearningStep)
    }
  }

  def validateLearningStep(
      newLearningStep: LearningStep,
      learningPath: LearningPath,
      allowUnknownLanguage: Boolean,
  ): Seq[ValidationMessage] = {
    validateSupportedLanguages(newLearningStep, learningPath) ++
      titleValidator.validate(newLearningStep.title, allowUnknownLanguage) ++
      validateIntroduction(newLearningStep.introduction, allowUnknownLanguage) ++
      validateDescription(newLearningStep.description, allowUnknownLanguage) ++
      validateEmbedUrl(newLearningStep.embedUrl, allowUnknownLanguage) ++
      validateLicense(newLearningStep.copyright.map(_.license)).toList ++
      validateThatDescriptionOrEmbedUrlOrArticleIdIsDefined(newLearningStep).toList
  }

  private def validateSupportedLanguages(learningStep: LearningStep, learningPath: LearningPath) =
    (learningStep.supportedLanguages.size, learningPath.isMyNDLAOwner) match {
      case (1, true)  => List()
      case (_, true)  => List(ValidationMessage("supportedLanguages", MY_NDLA_INVALID_LANGUAGES))
      case (_, false) => List()
    }

  def validateIntroduction(introductions: Seq[Introduction], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
    if (introductions.isEmpty) {
      List()
    } else {
      introductions.flatMap(introduction => {
        allowedHtmlTextValidator.validate("introduction", introduction.introduction).toList :::
          languageValidator.validate("language", introduction.language, allowUnknownLanguage).toList
      })
    }
  }

  def validateDescription(descriptions: Seq[Description], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
    if (descriptions.isEmpty) {
      List()
    } else {
      descriptions.flatMap(description => {
        allowedHtmlTextValidator.validate("description", description.description).toList :::
          languageValidator.validate("language", description.language, allowUnknownLanguage).toList
      })
    }
  }

  private def validateEmbedUrl(embedUrls: Seq[EmbedUrl], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
    embedUrls.flatMap(embedUrl => {
      urlValidator.validate("embedUrl.url", embedUrl.url).toList :::
        languageValidator.validate("language", embedUrl.language, allowUnknownLanguage).toList
    })
  }

  def validateLicense(licenseOpt: Option[String]): Option[ValidationMessage] = {
    licenseOpt match {
      case None          => None
      case Some(license) => noHtmlTextValidator.validate("license", license)
    }
  }

  private def validateThatDescriptionOrEmbedUrlOrArticleIdIsDefined(
      newLearningStep: LearningStep
  ): Option[ValidationMessage] = {
    if (newLearningStep.description.isEmpty && newLearningStep.embedUrl.isEmpty && newLearningStep.articleId.isEmpty) {
      Some(ValidationMessage("description|embedUrl|articleId", MISSING_DESCRIPTION_OR_EMBED_URL_OR_ARTICLE_ID))
    } else {
      None
    }
  }
}
