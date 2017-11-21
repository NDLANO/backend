package no.ndla.audioapi.service

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.DraftApiClient
import no.ndla.audioapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.audioapi.model.domain._
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Success, Try}

trait ValidationService {
  this: DraftApiClient =>
  val validationService: ValidationService

  class ValidationService {
    def validateAudioFile(audioFile: FileItem): Option[ValidationMessage] = {
      val validMimeTypes = Seq("audio/mp3", "audio/mpeg")
      val actualMimeType = audioFile.getContentType.getOrElse("")

      if (!validMimeTypes.contains(actualMimeType)) {
        return Some(ValidationMessage("files", s"The file ${audioFile.name} is not a valid audio file. Only valid types are '${validMimeTypes.mkString(",")}', but was '$actualMimeType'"))
      }

      audioFile.name.toLowerCase.endsWith(".mp3") match {
        case false => Some(ValidationMessage("files", s"The file ${audioFile.name} does not have a known file extension. Must be .mp3"))
        case true => None
      }
    }

    def validate(audio: AudioMetaInformation): Try[AudioMetaInformation] = {
      val validationMessages = validateNonEmpty("title", audio.titles).toSeq ++
        audio.titles.flatMap(title => validateNonEmpty("title", title.language)) ++
        audio.titles.flatMap(title => validateTitle("title", title)) ++
        validateCopyright(audio.copyright) ++
        validateTags(audio.tags)

      validationMessages match {
        case head :: tail => Failure(new ValidationException(errors=head :: tail))
        case _ => Success(audio)
      }
    }

    private def validateTitle(fieldPath: String, title: Title): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, title.title).toList ++
        validateLanguage(fieldPath, title.language)
    }

    def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      validateLicense(copyright.license).toList ++
      copyright.creators.flatMap(a => validateAuthor("copyright.creators", a, AudioApiProperties.creatorTypes)) ++
      copyright.processors.flatMap(a => validateAuthor("copyright.processors", a, AudioApiProperties.processorTypes)) ++
      copyright.rightsholders.flatMap(a => validateAuthor("copyright.rightsholders", a, AudioApiProperties.rightsholderType)) ++
      validateAgreement(copyright) ++
      copyright.origin.flatMap(origin => containsNoHtml("copyright.origin", origin))
    }

    def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    def validateAgreement(copyright: Copyright): Seq[ValidationMessage] = {
      copyright.agreementId match {
        case Some(id) =>
          draftApiClient.agreementExists(id) match {
            case false => Seq (ValidationMessage ("copyright.agreement", s"Agreement with id $id does not exist") )
            case _ => Seq()
          }
        case _ => Seq()
      }
    }

    def validateAuthor(fieldPath: String, author: Author, allowedTypes: Seq[String]): Seq[ValidationMessage] = {
      containsNoHtml(s"$fieldPath.type", author.`type`).toList ++
        containsNoHtml(s"$fieldPath.name", author.name).toList ++
        validateAuthorType(fieldPath, author.`type`, allowedTypes).toList
    }

    def validateAuthorType(fieldPath: String, `type`: String, allowedTypes: Seq[String]): Option[ValidationMessage] = {
      if(allowedTypes.contains(`type`.toLowerCase)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
    }

    def validateTags(tags: Seq[Tag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(containsNoHtml("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language).toList
      })
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Whitelist.none()) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed"))
      }
    }

    private def validateLanguage(fieldPath: String, languageCode: String): Option[ValidationMessage] = {
      languageCodeSupported6391(languageCode) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)

    private def validateNonEmpty(fieldPath: String, option: Option[_]): Option[ValidationMessage] = {
      option match {
        case Some(_) => None
        case None => Some(ValidationMessage(fieldPath, "There is no element to validate."))
      }
    }

    private def validateNonEmpty(fieldPath: String, sequence: Seq[Any]): Option[ValidationMessage] = {
      sequence.nonEmpty match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, "There are no elements to validate."))
      }
    }

  }
}
