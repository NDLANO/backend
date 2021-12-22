/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.validation

import no.ndla.draftapi.DraftApiProperties.{BrightcoveVideoScriptUrl, H5PResizerScriptUrl, NRKVideoScriptUrl}
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{ContentId, NewAgreementCopyright, NotFoundException, UpdatedArticle}
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.ConverterService
import no.ndla.language.model.Iso639
import no.ndla.mapping.License.getLicense
import no.ndla.validation._
import org.joda.time.format.ISODateTimeFormat

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ContentValidator {
  this: DraftRepository with ConverterService with ArticleApiClient =>
  val contentValidator: ContentValidator
  val importValidator: ContentValidator

  class ContentValidator() {
    private val NoHtmlValidator = new TextValidator(allowHtml = false)
    private val HtmlValidator = new TextValidator(allowHtml = true)

    def validate(content: Content): Try[Content] = {
      content match {
        case article: Article     => validateArticle(article)
        case agreement: Agreement => validateAgreement(agreement)
      }
    }

    def validateAgreement(agreement: Agreement,
                          preExistingErrors: Seq[ValidationMessage] = Seq.empty): Try[Agreement] = {
      val validationErrors = NoHtmlValidator.validate("title", agreement.title).toList ++
        NoHtmlValidator.validate("content", agreement.content).toList ++
        preExistingErrors.toList ++
        validateAgreementCopyright(agreement.copyright)

      if (validationErrors.isEmpty) {
        Success(agreement)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    def validateDates(newCopyright: NewAgreementCopyright): Seq[ValidationMessage] = {
      newCopyright.validFrom.map(dateString => validateDate("copyright.validFrom", dateString)).toSeq.flatten ++
        newCopyright.validTo.map(dateString => validateDate("copyright.validTo", dateString)).toSeq.flatten
    }

    def validateDate(fieldName: String, dateString: String): Seq[ValidationMessage] = {
      val parser = ISODateTimeFormat.dateOptionalTimeParser()
      Try(parser.parseDateTime(dateString)) match {
        case Success(_) => Seq.empty
        case Failure(_) => Seq(ValidationMessage(fieldName, "Date field needs to be in ISO 8601"))
      }

    }

    def validateArticle(article: Article): Try[Article] = {
      val validationErrors = article.content.flatMap(c => validateArticleContent(c)) ++
        article.introduction.flatMap(i => validateIntroduction(i)) ++
        article.metaDescription.flatMap(m => validateMetaDescription(m)) ++
        validateTitles(article.title) ++
        article.copyright.map(x => validateCopyright(x)).toSeq.flatten ++
        validateTags(article.tags) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImage.flatMap(validateMetaImage) ++
        article.visualElement.flatMap(v => validateVisualElement(v))

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }

    }

    def validateArticleApiArticle(id: Long, importValidate: Boolean): Try[ContentId] = {
      draftRepository.withId(id) match {
        case None => Failure(NotFoundException(s"Article with id $id does not exist"))
        case Some(art) =>
          articleApiClient
            .validateArticle(converterService.toArticleApiArticle(art), importValidate)
            .map(_ => ContentId(id))
      }
    }

    def validateArticleApiArticle(id: Long,
                                  updatedArticle: UpdatedArticle,
                                  importValidate: Boolean,
                                  user: UserInfo): Try[ContentId] = {
      draftRepository.withId(id) match {
        case None => Failure(NotFoundException(s"Article with id $id does not exist"))
        case Some(existing) =>
          converterService
            .toDomainArticle(existing, updatedArticle, isImported = false, user, None, None)
            .map(converterService.toArticleApiArticle)
            .flatMap(articleApiClient.validateArticle(_, importValidate))
            .map(_ => ContentId(id))
      }
    }

    private def validateArticleContent(content: ArticleContent): Seq[ValidationMessage] = {
      HtmlValidator.validate("content", content.content).toList ++
        rootElementContainsOnlySectionBlocks("content.content", content.content) ++
        validateLanguage("content.language", content.language)
    }

    def rootElementContainsOnlySectionBlocks(field: String, html: String): Option[ValidationMessage] = {
      val legalTopLevelTag = "section"
      val topLevelTags = HtmlTagRules.stringToJsoupDocument(html).children().asScala.map(_.tagName())

      if (topLevelTags.forall(_ == legalTopLevelTag)) {
        None
      } else {
        val illegalTags = topLevelTags.filterNot(_ == legalTopLevelTag).mkString(",")
        Some(
          ValidationMessage(
            field,
            s"An article must consist of one or more <section> blocks. Illegal tag(s) are $illegalTags "))
      }
    }

    private def validateVisualElement(content: VisualElement): List[ValidationMessage] = {
      HtmlValidator
        .validate("visualElement", content.resource, requiredToOptional = Map("image" -> Seq("data-caption")))
        .toList ++
        validateLanguage("language", content.language)
    }

    private def validateIntroduction(content: ArticleIntroduction): List[ValidationMessage] = {
      NoHtmlValidator.validate("introduction", content.introduction).toList ++
        validateLanguage("language", content.language)
    }

    private def validateMetaDescription(content: ArticleMetaDescription): List[ValidationMessage] = {
      NoHtmlValidator.validate("metaDescription", content.content).toList ++
        validateLanguage("language", content.language)
    }

    private def validateTitles(titles: Seq[ArticleTitle]): Seq[ValidationMessage] = {
      if (titles.isEmpty)
        Seq(
          ValidationMessage(
            "title",
            "An article must contain at least one title. Perhaps you tried to delete the only title in the article?"))
      else
        titles.flatMap(t => validateTitle(t.title, t.language))
    }

    private def validateTitle(title: String, language: String): Seq[ValidationMessage] = {
      NoHtmlValidator.validate(s"title.$language", title).toList ++
        validateLanguage("language", language) ++
        validateLength(s"title.$language", title, 256) ++
        validateMinimumLength(s"title.$language", title, 1)
    }

    private def validateAgreementCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val agreementMessage = copyright.agreementId
        .map(_ => ValidationMessage("copyright.agreementId", "Agreement copyrights cant contain agreements"))
        .toSeq
      agreementMessage ++ validateCopyright(copyright)
    }

    private def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = copyright.license.map(validateLicense).toSeq.flatten
      val contributorsMessages = copyright.creators.flatMap(validateAuthor) ++ copyright.processors.flatMap(
        validateAuthor) ++ copyright.rightsholders.flatMap(validateAuthor)
      val originMessage =
        copyright.origin.map(origin => NoHtmlValidator.validate("copyright.origin", origin)).toSeq.flatten

      licenseMessage ++ contributorsMessages ++ originMessage
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("author.type", author.`type`).toList ++
        NoHtmlValidator.validate("author.name", author.name).toList
    }

    private def validateTags(tags: Seq[ArticleTag]) = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(NoHtmlValidator.validate("tags", _)).toList :::
          validateLanguage("language", tagList.language).toList
      })
    }

    private def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(BrightcoveVideoScriptUrl, H5PResizerScriptUrl) ++ NRKVideoScriptUrl
      if (permittedLibraries.contains(requiredLibrary.url)) {
        None
      } else {
        Some(ValidationMessage(
          "requiredLibraries.url",
          s"${requiredLibrary.url} is not a permitted script. Allowed scripts are: ${permittedLibraries.mkString(",")}"))
      }
    }

    private def validateMetaImage(metaImage: ArticleMetaImage): Seq[ValidationMessage] =
      (validateMetaImageId(metaImage.imageId) ++ validateMetaImageAltText(metaImage.altText)).toSeq

    private def validateMetaImageAltText(altText: String): Seq[ValidationMessage] =
      NoHtmlValidator.validate("metaImage.alt", altText)

    private def validateMetaImageId(id: String): Option[ValidationMessage] = {
      def isAllDigits(x: String) = x forall Character.isDigit
      if (isAllDigits(id) && id.nonEmpty) {
        None
      } else {
        Some(ValidationMessage("metaImageId", "Meta image ID must be a number"))
      }
    }

    private def validateLanguage(fieldPath: String, languageCode: String) = {
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
          ValidationMessage(fieldPath,
                            s"This field does not meet the minimum length requirement of $minLength characters"))
      else
        None

    private def languageCodeSupported639(languageCode: String): Boolean = Iso639.get(languageCode).isSuccess

  }
}
