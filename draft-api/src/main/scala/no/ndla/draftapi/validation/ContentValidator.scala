/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.validation

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain._
import no.ndla.common.model.domain.draft._
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{ContentId, NotFoundException, UpdatedArticle}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.ConverterService
import no.ndla.language.model.Iso639
import no.ndla.mapping.License.getLicense
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.HtmlTagRules.{allLegalTags, stringToJsoupDocument}
import no.ndla.validation.SlugValidator.validateSlug
import no.ndla.validation._
import scalikejdbc.ReadOnlyAutoSession

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ContentValidator {
  this: DraftRepository with ConverterService with ArticleApiClient with Props =>
  val contentValidator: ContentValidator
  val importValidator: ContentValidator

  class ContentValidator() {
    import props.{BrightcoveVideoScriptUrl, H5PResizerScriptUrl, NRKVideoScriptUrl}
    private val inlineHtmlTags       = props.InlineHtmlTags
    private val introductionHtmlTags = props.IntroductionHtmlTags

    def validateDate(fieldName: String, dateString: String): Seq[ValidationMessage] = {
      NDLADate.fromString(dateString) match {
        case Success(_) => Seq.empty
        case Failure(_) => Seq(ValidationMessage(fieldName, "Date field needs to be in ISO 8601"))
      }

    }

    private def validateResponsible(draft: Draft): Option[ValidationMessage] = {
      val statusRequiresResponsible = DraftStatus.thatRequiresResponsible.contains(draft.status.current)
      Option.when(draft.responsible.isEmpty && statusRequiresResponsible) {
        ValidationMessage(
          "responsibleId",
          s"Responsible needs to be set if the status is not ${DraftStatus.thatDoesNotRequireResponsible}"
        )
      }
    }

    def validateArticleOnLanguage(oldArticle: Option[Draft], article: Draft, language: Option[String]): Try[Draft] = {
      val toValidate    = language.map(getArticleOnLanguage(article, _)).getOrElse(article)
      val oldToValidate = language.map(getArticleOnLanguage(article, _)).orElse(oldArticle)
      validateArticle(oldToValidate, toValidate)
    }

    def validateArticleOnLanguage(article: Draft, language: Option[String]): Try[Draft] =
      validateArticleOnLanguage(None, article, language)

    private def getArticleOnLanguage(article: Draft, language: String): Draft = {
      article.copy(
        content = article.content.filter(_.language == language),
        introduction = article.introduction.filter(_.language == language),
        metaDescription = article.metaDescription.filter(_.language == language),
        title = article.title.filter(_.language == language),
        tags = article.tags.filter(_.language == language),
        visualElement = article.visualElement.filter(_.language == language),
        metaImage = article.metaImage.filter(_.language == language)
      )
    }

    def validateArticle(article: Draft): Try[Draft] = validateArticle(None, article)

    def validateArticle(oldArticle: Option[Draft], article: Draft): Try[Draft] = {
      val shouldValidateEntireArticle = !onlyUpdatedEditorialFields(oldArticle, article)
      val regularValidationErrors =
        if (shouldValidateEntireArticle)
          article.content.flatMap(c => validateArticleContent(c)) ++
            article.introduction.flatMap(i => validateIntroduction(i)) ++
            article.metaDescription.flatMap(m => validateMetaDescription(m)) ++
            validateTitles(article.title) ++
            article.copyright.map(x => validateCopyright(x)).toSeq.flatten ++
            validateTags(article.tags) ++
            article.requiredLibraries.flatMap(validateRequiredLibrary) ++
            article.metaImage.flatMap(validateMetaImage) ++
            article.visualElement.flatMap(v => validateVisualElement(v)) ++
            validateSlug(article.slug, article.articleType, article.id, draftRepository.slugExists) ++
            validateResponsible(article)
        else Seq.empty

      val editorialValidationErrors =
        validateRevisionMeta(article.revisionMeta)

      val validationErrors = regularValidationErrors ++ editorialValidationErrors

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }

    }

    private def onlyUpdatedEditorialFields(existingArticle: Option[Draft], changedArticle: Draft): Boolean = {
      existingArticle match {
        case None => false
        case Some(oldArticle) =>
          val withComparableValues =
            (article: Draft) =>
              converterService
                .withSortedLanguageFields(article)
                .copy(
                  revision = None,
                  notes = Seq.empty,
                  editorLabels = Seq.empty,
                  comments = List.empty,
                  updated = NDLADate.MIN,
                  revisionMeta = Seq.empty,
                  updatedBy = ""
                )

          withComparableValues(oldArticle) == withComparableValues(changedArticle)
      }
    }

    def validateArticleApiArticle(id: Long, importValidate: Boolean, user: TokenUser): Try[ContentId] = {
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case None => Failure(NotFoundException(s"Article with id $id does not exist"))
        case Some(draft) =>
          converterService
            .toArticleApiArticle(draft)
            .flatMap(article => articleApiClient.validateArticle(article, importValidate, Some(user)))
            .map(_ => ContentId(id))
      }
    }

    def validateArticleApiArticle(
        id: Long,
        updatedArticle: UpdatedArticle,
        importValidate: Boolean,
        user: TokenUser
    ): Try[ContentId] = {
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case None => Failure(NotFoundException(s"Article with id $id does not exist"))
        case Some(existing) =>
          converterService
            .toDomainArticle(existing, updatedArticle, isImported = false, user, None, None)
            .flatMap(converterService.toArticleApiArticle)
            .flatMap(articleApiClient.validateArticle(_, importValidate, Some(user)))
            .map(_ => ContentId(id))
      }
    }

    private def validateArticleContent(content: ArticleContent): Seq[ValidationMessage] = {
      TextValidator.validate("content", content.content, allLegalTags).toList ++
        rootElementContainsOnlySectionBlocks("content.content", content.content) ++
        validateLanguage("content.language", content.language)
    }

    private def rootElementContainsOnlySectionBlocks(field: String, html: String): Option[ValidationMessage] = {
      val legalTopLevelTag = "section"
      val topLevelTags     = stringToJsoupDocument(html).children().asScala.map(_.tagName())

      if (topLevelTags.forall(_ == legalTopLevelTag)) {
        None
      } else {
        val illegalTags = topLevelTags.filterNot(_ == legalTopLevelTag).mkString(",")
        Some(
          ValidationMessage(
            field,
            s"An article must consist of one or more <section> blocks. Illegal tag(s) are $illegalTags "
          )
        )
      }
    }

    private def validateVisualElement(content: VisualElement): List[ValidationMessage] = {
      TextValidator
        .validateVisualElement(
          "visualElement",
          content.resource,
          allLegalTags,
          requiredToOptional = Map("image" -> Seq("data-caption"))
        )
        .toList ++ validateLanguage("language", content.language)
    }

    private def validateRevisionMeta(revisionMeta: Seq[RevisionMeta]): Seq[ValidationMessage] = {
      revisionMeta.find(rm =>
        rm.status == RevisionStatus.NeedsRevision && rm.revisionDate.isAfter(NDLADate.now())
      ) match {
        case Some(_) => Seq.empty
        case None =>
          Seq(
            ValidationMessage(
              "revisionMeta",
              "An article must contain at least one planned revisiondate"
            )
          )
      }
    }

    private def validateIntroduction(content: Introduction): List[ValidationMessage] = {
      TextValidator.validate("introduction", content.introduction, introductionHtmlTags).toList ++
        validateLanguage("language", content.language)
    }

    private def validateMetaDescription(content: Description): List[ValidationMessage] = {
      TextValidator.validate("metaDescription", content.content, Set.empty).toList ++
        validateLanguage("language", content.language)
    }

    private def validateTitles(titles: Seq[Title]): Seq[ValidationMessage] = {
      if (titles.isEmpty)
        Seq(
          ValidationMessage(
            "title",
            "An article must contain at least one title. Perhaps you tried to delete the only title in the article?"
          )
        )
      else
        titles.flatMap(t => validateTitle(t.title, t.language))
    }

    private def validateTitle(title: String, language: String): Seq[ValidationMessage] = {
      TextValidator.validate(s"title.$language", title, inlineHtmlTags).toList ++
        validateLanguage("language", language) ++
        validateLength(s"title.$language", title, 256) ++
        validateMinimumLength(s"title.$language", title, 1)
    }

    private def validateCopyright(copyright: DraftCopyright): Seq[ValidationMessage] = {
      val licenseMessage = copyright.license.map(validateLicense).toSeq.flatten
      val contributorsMessages = copyright.creators.flatMap(validateAuthor) ++ copyright.processors.flatMap(
        validateAuthor
      ) ++ copyright.rightsholders.flatMap(validateAuthor)
      val originMessage =
        copyright.origin.map(origin => TextValidator.validate("copyright.origin", origin, Set.empty)).toSeq.flatten

      licenseMessage ++ contributorsMessages ++ originMessage
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      TextValidator.validate("author.type", author.`type`, Set.empty).toList ++
        TextValidator.validate("author.name", author.name, Set.empty).toList
    }

    private def validateTags(tags: Seq[Tag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(TextValidator.validate("tags", _, Set.empty)).toList :::
          validateLanguage("language", tagList.language).toList
      })
    }

    private def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(BrightcoveVideoScriptUrl, H5PResizerScriptUrl) ++ NRKVideoScriptUrl
      if (permittedLibraries.contains(requiredLibrary.url)) {
        None
      } else {
        Some(
          ValidationMessage(
            "requiredLibraries.url",
            s"${requiredLibrary.url} is not a permitted script. Allowed scripts are: ${permittedLibraries.mkString(",")}"
          )
        )
      }
    }

    private def validateMetaImage(metaImage: ArticleMetaImage): Seq[ValidationMessage] =
      (validateMetaImageId(metaImage.imageId) ++ validateMetaImageAltText(metaImage.altText)).toSeq

    private def validateMetaImageAltText(altText: String): Seq[ValidationMessage] =
      TextValidator.validate("metaImage.alt", altText, Set.empty)

    private def validateMetaImageId(id: String): Option[ValidationMessage] = {
      def isAllDigits(x: String) = x forall Character.isDigit
      if (isAllDigits(id) && id.nonEmpty) {
        None
      } else {
        Some(ValidationMessage("metaImageId", "Meta image ID must be a number"))
      }
    }

    private def validateLanguage(fieldPath: String, languageCode: String): Option[ValidationMessage] = {
      if (languageCode.nonEmpty && languageCodeSupported639(languageCode)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
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
