/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.{Author, ContributorType, Tag, UploadedFile}
import no.ndla.common.model.domain as commonDomain
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.*
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

import scala.util.{Failure, Success, Try}

trait ValidationService {
  this: Props =>
  lazy val validationService: ValidationService

  class ValidationService {

    def validateImageFile(imageFile: UploadedFile): Option[ValidationMessage] = {
      val fn = imageFile.fileName.getOrElse("").stripPrefix("\"").stripSuffix("\"")
      if (!hasValidFileExtension(fn, props.ValidFileExtensions))
        return Some(
          ValidationMessage(
            "file",
            s"The file $fn does not have a known file extension. Must be one of ${props.ValidFileExtensions
                .mkString(",")}"
          )
        )

      val actualMimeType = imageFile.contentType.getOrElse("")

      if (!props.ValidMimeTypes.contains(actualMimeType))
        return Some(
          ValidationMessage(
            "file",
            s"The file $fn is not a valid image file. Only valid type is '${props.ValidMimeTypes.mkString(",")}', but was '$actualMimeType'"
          )
        )

      None
    }

    private def hasValidFileExtension(filename: String, extensions: Seq[String]): Boolean = {
      extensions.exists(extension => filename.toLowerCase.endsWith(extension))
    }

    def validate(image: ImageMetaInformation, oldImage: Option[ImageMetaInformation]): Try[ImageMetaInformation] = {
      val oldTitleLanguages   = oldImage.map(_.titles.map(_.language)).getOrElse(Seq())
      val oldTagLanguages     = oldImage.map(_.tags.map(_.language)).getOrElse(Seq())
      val oldAltTextLanguages = oldImage.map(_.alttexts.map(_.language)).getOrElse(Seq())
      val oldCaptionLanguages = oldImage.map(_.captions.map(_.language)).getOrElse(Seq())

      val oldLanguages = (oldTitleLanguages ++ oldTagLanguages ++ oldAltTextLanguages ++ oldCaptionLanguages).distinct

      val validationMessages = image.titles.flatMap(title => validateTitle("title", title, oldLanguages)) ++
        validateCopyright(image.copyright) ++
        validateTags(image.tags, oldLanguages) ++
        image.alttexts.flatMap(alt => validateAltText("altTexts", alt, oldLanguages)) ++
        image.captions.flatMap(caption => validateCaption("captions", caption, oldLanguages))

      if (validationMessages.isEmpty) Success(image)
      else Failure(new ValidationException(errors = validationMessages))
    }

    private def validateTitle(
        fieldPath: String,
        title: ImageTitle,
        oldLanguages: Seq[String]
    ): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, title.title).toList ++
        validateLanguage(fieldPath, title.language, oldLanguages)
    }

    private def validateAltText(
        fieldPath: String,
        altText: ImageAltText,
        oldLanguages: Seq[String]
    ): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, altText.alttext).toList ++
        validateLanguage(fieldPath, altText.language, oldLanguages)
    }

    private def validateCaption(
        fieldPath: String,
        caption: ImageCaption,
        oldLanguages: Seq[String]
    ): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, caption.caption).toList ++
        validateLanguage(fieldPath, caption.language, oldLanguages)
    }

    def validateCopyright(copyright: commonDomain.article.Copyright): Seq[ValidationMessage] = {
      validateLicense(copyright.license).toList ++
        validateAuthorLicenseCorrelation(
          Some(copyright.license),
          copyright.rightsholders ++ copyright.creators ++ copyright.processors
        ) ++
        copyright.creators.flatMap(a => validateAuthor("copyright.creators", a, ContributorType.creators)) ++
        copyright.processors.flatMap(a => validateAuthor("copyright.processors", a, ContributorType.processors)) ++
        copyright.rightsholders.flatMap(a =>
          validateAuthor("copyright.rightsholders", a, ContributorType.rightsholders)
        ) ++
        copyright.origin.flatMap(origin => containsNoHtml("copyright.origin", origin))
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    private def validateAuthorLicenseCorrelation(license: Option[String], authors: Seq[Author]) = {
      val errorMessage = (lic: String) =>
        ValidationMessage("license.license", s"At least one copyright holder is required when license is $lic")
      license match {
        case None      => Seq()
        case Some(lic) => if (lic == "N/A" || authors.nonEmpty) Seq() else Seq(errorMessage(lic))
      }
    }

    private def validateAuthor(
        fieldPath: String,
        author: Author,
        allowedTypes: Seq[ContributorType]
    ): Seq[ValidationMessage] = {
      containsNoHtml(s"$fieldPath.name", author.name).toList ++
        validateAuthorType(s"$fieldPath.type", author.`type`, allowedTypes) ++
        validateMinimumLength(s"$fieldPath.name", author.name, 1)
    }

    private def validateAuthorType(
        fieldPath: String,
        `type`: ContributorType,
        allowedTypes: Seq[ContributorType]
    ): Option[ValidationMessage] = {
      if (allowedTypes.contains(`type`)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
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

    private def validateTags(tags: Seq[Tag], oldLanguages: Seq[String]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(containsNoHtml("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language, oldLanguages).toList
      })
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (Jsoup.isValid(text, Safelist.none())) {
        None
      } else {
        Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed"))
      }
    }

    private def validateLanguage(
        fieldPath: String,
        languageCode: String,
        oldLanguages: Seq[String]
    ): Option[ValidationMessage] = {

      if (languageCodeSupported6391(languageCode) || oldLanguages.contains(languageCode)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)

  }
}
