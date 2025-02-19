/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.validation

import no.ndla.common.model.domain.{Author, Title}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.concept.{Concept, ConceptContent, ConceptMetaImage, ConceptStatus, VisualElement}
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.repository.DraftConceptRepository
import no.ndla.conceptapi.service.ConverterService
import no.ndla.conceptapi.validation.GlossDataValidator.validateGlossData
import no.ndla.language.model.{Iso639, WithLanguage}
import no.ndla.mapping.License.getLicense
import no.ndla.validation.HtmlTagRules.allLegalTags
import no.ndla.validation.*

import scala.util.{Failure, Success, Try}

trait ContentValidator {
  this: DraftConceptRepository & ConverterService & Props =>
  val contentValidator: ContentValidator

  class ContentValidator {

    def validateConcept(concept: Concept): Try[Concept] = {
      val validationErrors =
        concept.content.flatMap(c => validateConceptContent(c)) ++
          concept.visualElement.flatMap(ve => validateVisualElement(ve)) ++
          concept.metaImage.flatMap(mi => validateMetaImage(mi)) ++
          validateTitles(concept.title) ++
          concept.copyright.map(co => validateCopyright(co)).getOrElse(Seq()) ++
          validateResponsible(concept) ++
          validateGlossData(concept.glossData, concept.conceptType)

      if (validationErrors.isEmpty) {
        Success(concept)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    private def validateResponsible(concept: Concept): Option[ValidationMessage] = {
      val statusRequiresResponsible = ConceptStatus.thatRequiresResponsible.contains(concept.status.current)
      Option.when(concept.responsible.isEmpty && statusRequiresResponsible) {
        ValidationMessage(
          "responsibleId",
          s"Responsible needs to be set if the status is not ${ConceptStatus.thatDoesNotRequireResponsible}"
        )
      }
    }

    private def validateMetaImage(metaImage: ConceptMetaImage): Seq[ValidationMessage] = {
      validateMinimumLength(s"metaImage.id", metaImage.imageId, 1).toSeq
    }

    private def validateVisualElement(content: VisualElement): Seq[ValidationMessage] = {
      TextValidator
        .validateVisualElement(
          "visualElement",
          content.visualElement,
          allLegalTags,
          requiredToOptional = Map("image" -> Seq("data-caption"))
        )
        .toList ++
        validateLanguage("language", content.language)
    }

    private def validateConceptContent(content: ConceptContent): Seq[ValidationMessage] = {
      TextValidator.validate("content", content.content, props.IntroductionHtmlTags).toList ++
        validateLanguage("language", content.language)
    }

    private def validateTitles(titles: Seq[Title]): Seq[ValidationMessage] = {
      titles.flatMap(t => validateTitle(t.title, t.language)) ++
        validateExistingLanguageField("title", titles)
    }

    private def validateTitle(title: String, language: String): Seq[ValidationMessage] = {
      TextValidator.validate(s"title.$language", title, Set.empty).toList ++
        validateLanguage("language", language) ++
        validateLength(s"title.$language", title, 256) ++
        validateMinimumLength(s"title.$language", title, 1)
    }

    private def validateCopyright(copyright: DraftCopyright): Seq[ValidationMessage] = {
      val licenseMessage            = copyright.license.map(validateLicense).toSeq.flatten
      val allAuthors                = copyright.creators ++ copyright.processors ++ copyright.rightsholders
      val licenseCorrelationMessage = validateAuthorLicenseCorrelation(copyright.license, allAuthors)
      val contributorsMessages = copyright.creators.flatMap(validateAuthor) ++ copyright.processors
        .flatMap(validateAuthor) ++ copyright.rightsholders.flatMap(validateAuthor)
      val originMessage =
        copyright.origin
          .map(origin => TextValidator.validate("copyright.origin", origin, Set.empty))
          .toSeq
          .flatten

      licenseMessage ++ licenseCorrelationMessage ++ contributorsMessages ++ originMessage
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None =>
          Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    private def validateAuthorLicenseCorrelation(
        license: Option[String],
        authors: Seq[Author]
    ): Seq[ValidationMessage] = {
      val errorMessage = (lic: String) =>
        ValidationMessage("license.license", s"At least one copyright holder is required when license is $lic")
      license match {
        case None      => Seq()
        case Some(lic) => if (lic == "N/A" || authors.nonEmpty) Seq() else Seq(errorMessage(lic))
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      TextValidator.validate("author.type", author.`type`, Set.empty).toList ++
        TextValidator.validate("author.name", author.name, Set.empty).toList
    }

    private def validateLanguage(fieldPath: String, languageCode: String): Option[ValidationMessage] = {
      if (languageCode.nonEmpty && languageCodeSupported639(languageCode)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def validateExistingLanguageField(
        fieldPath: String,
        fields: Seq[WithLanguage]
    ): Option[ValidationMessage] = {
      if (fields.nonEmpty) None
      else
        Some(
          ValidationMessage(
            fieldPath,
            s"The field does not have any entries, whereas at least one is required."
          )
        )
    }

    private def validateLength(fieldPath: String, content: String, maxLength: Int): Option[ValidationMessage] = {
      if (content.length > maxLength)
        Some(ValidationMessage(fieldPath, s"This field exceeds the maximum permitted length of $maxLength characters"))
      else
        None
    }

    private def validateMinimumLength(fieldPath: String, content: String, minLength: Int): Option[ValidationMessage] =
      if (content.trim.length < minLength)
        Some(
          ValidationMessage(
            fieldPath,
            s"This field does not meet the minimum length requirement of $minLength characters"
          )
        )
      else
        None

    private def languageCodeSupported639(languageCode: String): Boolean = Iso639.get(languageCode).isSuccess

  }
}
