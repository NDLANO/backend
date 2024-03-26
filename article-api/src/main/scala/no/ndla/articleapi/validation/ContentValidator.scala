/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.Props
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.{Article, Copyright}
import no.ndla.common.model.domain._
import no.ndla.language.model.{Iso639, LanguageField}
import no.ndla.mapping.License.getLicense
import no.ndla.validation.HtmlTagRules.{allLegalTags, stringToJsoupDocument}
import no.ndla.validation.SlugValidator.validateSlug
import no.ndla.validation.TextValidator

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ContentValidator {
  this: ArticleRepository with Props =>
  val contentValidator: ContentValidator

  class ContentValidator() {
    private val inlineHtmlTags       = props.InlineHtmlTags
    private val introductionHtmlTags = props.IntroductionHtmlTags

    def softValidateArticle(article: Article, isImported: Boolean): Try[Article] = {
      val metaValidation =
        if (isImported) None else validateNonEmpty("metaDescription", article.metaDescription)
      val validationErrors =
        validateRevisionDate(article.revisionDate) ++
          validateNonEmpty("content", article.content) ++
          validateNonEmpty("title", article.title) ++
          metaValidation

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    def validateArticle(article: Article, isImported: Boolean): Try[Article] = {
      val validationErrors = validateArticleContent(article.content) ++
        article.introduction.flatMap(i => validateIntroduction(i)) ++
        validateMetaDescription(article.metaDescription, isImported) ++
        validateTitle(article.title) ++
        validateCopyright(article.copyright) ++
        validateTags(article.tags, isImported) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImage.flatMap(validateMetaImage) ++
        article.visualElement.flatMap(v => validateVisualElement(v)) ++
        validateRevisionDate(article.revisionDate) ++
        validateSlug(article.slug, article.articleType, article.id, articleRepository.slugExists)
      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    private def validateRevisionDate(revisionDate: Option[NDLADate]): Seq[ValidationMessage] = {
      revisionDate match {
        case None => Seq(ValidationMessage("revisionDate", "Article must have at least one unfinished revision"))
        case _    => Seq.empty
      }
    }

    private def validateNonEmpty(field: String, values: Seq[LanguageField[_]]): Option[ValidationMessage] = {
      if (values.isEmpty || values.forall(_.isEmpty)) {
        Some(ValidationMessage(field, "Field must contain at least one entry"))
      } else
        None
    }

    private def validateArticleContent(contents: Seq[ArticleContent]): Seq[ValidationMessage] = {
      contents.flatMap(content => {
        val field = s"content.${content.language}"
        TextValidator.validate(field, content.content, allLegalTags).toList ++
          rootElementContainsOnlySectionBlocks(field, content.content) ++
          validateLanguage("content.language", content.language)
      }) ++ validateNonEmpty("content", contents)
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

    private def validateVisualElement(content: VisualElement): Seq[ValidationMessage] = {
      val field = s"visualElement.${content.language}"
      TextValidator
        .validateVisualElement(
          field,
          content.resource,
          allLegalTags,
          requiredToOptional = Map("image" -> Seq("data-caption"))
        )
        .toList ++
        validateLanguage("visualElement.language", content.language)
    }

    private def validateIntroduction(content: Introduction): Seq[ValidationMessage] = {
      val field = s"introduction.${content.language}"
      TextValidator.validate(field, content.introduction, introductionHtmlTags).toList ++
        validateLanguage("introduction.language", content.language)
    }

    private def validateMetaDescription(
        contents: Seq[Description],
        allowEmpty: Boolean
    ): Seq[ValidationMessage] = {
      val nonEmptyValidation = if (allowEmpty) None else validateNonEmpty("metaDescription", contents)
      val validations = contents.flatMap(content => {
        val field = s"metaDescription.${content.language}"
        TextValidator.validate(field, content.content, Set.empty).toList ++
          validateLanguage("metaDescription.language", content.language)
      })
      validations ++ nonEmptyValidation
    }

    private def validateTitle(titles: Seq[LanguageField[String]]): Seq[ValidationMessage] = {
      titles.flatMap(title => {
        val field = s"title.$language"
        TextValidator.validate(field, title.value, inlineHtmlTags).toList ++
          validateLanguage("title.language", title.language) ++
          validateLength("title", title.value, 256)
      }) ++ validateNonEmpty("title", titles)
    }

    private def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage            = validateLicense(copyright.license)
      val allAuthors                = copyright.creators ++ copyright.processors ++ copyright.rightsholders
      val licenseCorrelationMessage = validateAuthorLicenseCorrelation(copyright.license, allAuthors)
      val contributorsMessages =
        copyright.creators.flatMap(a => validateAuthor(a, "copyright.creators", props.creatorTypes)) ++
          copyright.processors.flatMap(a => validateAuthor(a, "copyright.processors", props.processorTypes)) ++
          copyright.rightsholders.flatMap(a => validateAuthor(a, "copyright.rightsholders", props.rightsholderTypes))
      val originMessage =
        copyright.origin
          .map(origin => TextValidator.validate("copyright.origin", origin, Set.empty))
          .getOrElse(Seq.empty)

      licenseMessage ++ licenseCorrelationMessage ++ contributorsMessages ++ originMessage
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    private def validateAuthorLicenseCorrelation(license: String, authors: Seq[Author]) = {
      val errorMessage = (lic: String) =>
        ValidationMessage("license.license", s"At least one copyright holder is required when license is $lic")
      if (license == "N/A" || authors.nonEmpty) Seq() else Seq(errorMessage(license))
    }

    private def validateAuthor(author: Author, fieldPath: String, allowedTypes: Seq[String]): Seq[ValidationMessage] = {
      TextValidator.validate(s"$fieldPath.type", author.`type`, Set.empty).toList ++
        TextValidator.validate(s"$fieldPath.name", author.name, Set.empty).toList ++
        validateAuthorType(s"$fieldPath.type", author.`type`, allowedTypes).toList
    }

    private def validateAuthorType(
        fieldPath: String,
        `type`: String,
        allowedTypes: Seq[String]
    ): Option[ValidationMessage] = {
      if (allowedTypes.contains(`type`.toLowerCase)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
    }

    private def validateTags(tags: Seq[Tag], isImported: Boolean): Seq[ValidationMessage] = {

      // Since quite a few articles from old ndla has fewer than 3 tags we skip validation here for imported articles until we are done importing.
      val languageTagAmountErrors = tags.groupBy(_.language).flatMap {
        case (lang, tagsForLang) if !isImported && tagsForLang.flatMap(_.tags).size < props.MinimumAllowedTags =>
          Seq(
            ValidationMessage(
              s"tags.$lang",
              s"Invalid amount of tags. Articles needs ${props.MinimumAllowedTags} or more tags to be valid."
            )
          )
        case _ => Seq()
      }

      val noTagsError =
        if (tags.isEmpty) Seq(ValidationMessage("tags", "The article must have at least one set of tags")) else Seq()

      tags.flatMap(tagList => {
        tagList.tags.flatMap(TextValidator.validate(s"tags.${tagList.language}", _, Set.empty)).toList :::
          validateLanguage("tags.language", tagList.language).toList
      }) ++ languageTagAmountErrors ++ noTagsError
    }

    private def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(props.BrightcoveVideoScriptUrl, props.H5PResizerScriptUrl) ++ props.NRKVideoScriptUrl
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
      if (languageCode.nonEmpty && languageCodeSupported6391(languageCode)) {
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

    private def languageCodeSupported6391(languageCode: String): Boolean = Iso639.get(languageCode).isSuccess
  }
}
