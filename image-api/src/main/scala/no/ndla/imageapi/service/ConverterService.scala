/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.typesafe.dsl.*
import io.lemonlabs.uri.UrlPath
import no.ndla.common.model.{domain as commonDomain, api as commonApi}
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.{
  ImageFileData,
  ImageFileDataDocument,
  ImageMetaInformation,
  ModelReleasedStatus,
  UploadedImage
}
import no.ndla.imageapi.model.{ImageConversionException, api, domain}
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.mapping.License.getLicense
import cats.implicits.*
import no.ndla.common.Clock
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock & Props =>
  lazy val converterService: ConverterService

  class ConverterService extends StrictLogging {
    def asApiAuthor(domainAuthor: commonDomain.Author): commonApi.AuthorDTO = {
      commonApi.AuthorDTO(domainAuthor.`type`, domainAuthor.name)
    }

    def asApiCopyright(domainCopyright: commonDomain.article.Copyright): commonApi.CopyrightDTO = {
      commonApi.CopyrightDTO(
        asApiLicense(domainCopyright.license),
        domainCopyright.origin,
        domainCopyright.creators.map(asApiAuthor),
        domainCopyright.processors.map(asApiAuthor),
        domainCopyright.rightsholders.map(asApiAuthor),
        domainCopyright.validFrom,
        domainCopyright.validTo,
        domainCopyright.processed
      )
    }

    def asApiImage(domainImage: ImageFileData, baseUrl: Option[String] = None): api.ImageDTO = {
      api.ImageDTO(baseUrl.getOrElse("") + domainImage.fileName, domainImage.size, domainImage.contentType)
    }

    def asApiImageAltText(domainImageAltText: domain.ImageAltText): api.ImageAltTextDTO = {
      api.ImageAltTextDTO(domainImageAltText.alttext, domainImageAltText.language)
    }

    def asApiImageMetaInformationWithApplicationUrlV2(
        domainImageMetaInformation: ImageMetaInformation,
        language: Option[String],
        user: Option[TokenUser]
    ): Try[api.ImageMetaInformationV2DTO] = {
      asImageMetaInformationV2(
        domainImageMetaInformation,
        language,
        props.ImageApiV2UrlBase,
        Some(props.RawImageUrlBase),
        user
      )
    }

    def asApiImageMetaInformationWithDomainUrlV2(
        domainImageMetaInformation: ImageMetaInformation,
        language: Option[String],
        user: Option[TokenUser]
    ): Try[api.ImageMetaInformationV2DTO] = {
      asImageMetaInformationV2(
        domainImageMetaInformation,
        language,
        props.ImageApiV2UrlBase,
        Some(props.RawImageUrlBase),
        user
      )
    }

    def asApiImageMetaInformationV3(
        imageMeta: ImageMetaInformation,
        language: Option[String],
        user: Option[TokenUser]
    ): Try[api.ImageMetaInformationV3DTO] = {
      val metaUrl = props.ImageApiV3UrlBase + imageMeta.id.get
      val rawPath = props.RawImageUrlBase
      val title   = findByLanguageOrBestEffort(imageMeta.titles, language)
        .map(asApiImageTitle)
        .getOrElse(api.ImageTitleDTO("", props.DefaultLanguage))
      val alttext = findByLanguageOrBestEffort(imageMeta.alttexts, language)
        .map(asApiImageAltText)
        .getOrElse(api.ImageAltTextDTO("", props.DefaultLanguage))
      val tags = findByLanguageOrBestEffort(imageMeta.tags, language)
        .map(asApiImageTag)
        .getOrElse(api.ImageTagDTO(Seq(), props.DefaultLanguage))
      val caption = findByLanguageOrBestEffort(imageMeta.captions, language)
        .map(asApiCaption)
        .getOrElse(api.ImageCaptionDTO("", props.DefaultLanguage))

      getImageFromMeta(imageMeta, language).flatMap(image => {
        val apiUrl       = asApiUrl(image.fileName, rawPath.some)
        val editorNotes  = Option.when(user.hasPermission(IMAGE_API_WRITE))(asApiEditorNotes(imageMeta.editorNotes))
        val apiImageFile = asApiImageFile(image, apiUrl)
        val supportedLanguages = getSupportedLanguages(imageMeta)

        Success(
          api
            .ImageMetaInformationV3DTO(
              id = imageMeta.id.get.toString,
              metaUrl = metaUrl,
              title = title,
              alttext = alttext,
              copyright = asApiCopyright(imageMeta.copyright),
              tags = tags,
              caption = caption,
              supportedLanguages = supportedLanguages,
              created = imageMeta.created,
              createdBy = imageMeta.createdBy,
              modelRelease = imageMeta.modelReleased.toString,
              editorNotes = editorNotes,
              image = apiImageFile
            )
        )
      })
    }

    private def asApiImageFile(image: ImageFileData, url: String): api.ImageFileDTO = {
      val dimensions = image.dimensions.map { case domain.ImageDimensions(width, height) =>
        api.ImageDimensionsDTO(width, height)
      }

      api.ImageFileDTO(
        fileName = image.fileName,
        size = image.size,
        contentType = image.contentType,
        imageUrl = url,
        dimensions = dimensions,
        language = image.language
      )
    }

    private def asApiEditorNotes(notes: Seq[domain.EditorNote]): Seq[api.EditorNoteDTO] = {
      notes.map(n => api.EditorNoteDTO(n.timeStamp, n.updatedBy, n.note))
    }

    private def getImageFromMeta(meta: ImageMetaInformation, language: Option[String]): Try[ImageFileData] = {
      findByLanguageOrBestEffort(meta.images.getOrElse(Seq.empty), language) match {
        case None =>
          Failure(ImageConversionException(s"Could not find image in meta with id '${meta.id}', this is a bug."))
        case Some(image) => Success(image)
      }
    }

    private[service] def asImageMetaInformationV2(
        imageMeta: ImageMetaInformation,
        language: Option[String],
        baseUrl: String,
        rawBaseUrl: Option[String],
        user: Option[TokenUser]
    ): Try[api.ImageMetaInformationV2DTO] = {
      val title = findByLanguageOrBestEffort(imageMeta.titles, language)
        .map(asApiImageTitle)
        .getOrElse(api.ImageTitleDTO("", props.DefaultLanguage))
      val alttext = findByLanguageOrBestEffort(imageMeta.alttexts, language)
        .map(asApiImageAltText)
        .getOrElse(api.ImageAltTextDTO("", props.DefaultLanguage))
      val tags = findByLanguageOrBestEffort(imageMeta.tags, language)
        .map(asApiImageTag)
        .getOrElse(api.ImageTagDTO(Seq(), props.DefaultLanguage))
      val caption = findByLanguageOrBestEffort(imageMeta.captions, language)
        .map(asApiCaption)
        .getOrElse(api.ImageCaptionDTO("", props.DefaultLanguage))

      getImageFromMeta(imageMeta, language).flatMap(image => {
        val apiUrl          = asApiUrl(image.fileName, rawBaseUrl)
        val editorNotes     = Option.when(user.hasPermission(IMAGE_API_WRITE))(asApiEditorNotes(imageMeta.editorNotes))
        val imageDimensions = image.dimensions.map(d => api.ImageDimensionsDTO(d.width, d.height))
        val supportedLanguages = getSupportedLanguages(imageMeta)

        Success(
          api
            .ImageMetaInformationV2DTO(
              id = imageMeta.id.get.toString,
              metaUrl = baseUrl + imageMeta.id.get,
              title = title,
              alttext = alttext,
              imageUrl = apiUrl,
              size = image.size,
              contentType = image.contentType,
              copyright = asApiCopyright(imageMeta.copyright),
              tags = tags,
              caption = caption,
              supportedLanguages = supportedLanguages,
              created = imageMeta.created,
              createdBy = imageMeta.createdBy,
              modelRelease = imageMeta.modelReleased.toString,
              editorNotes = editorNotes,
              imageDimensions = imageDimensions
            )
        )
      })
    }

    def asApiImageTag(domainImageTag: commonDomain.Tag): api.ImageTagDTO = {
      api.ImageTagDTO(domainImageTag.tags, domainImageTag.language)
    }

    def asApiCaption(domainImageCaption: domain.ImageCaption): api.ImageCaptionDTO =
      api.ImageCaptionDTO(domainImageCaption.caption, domainImageCaption.language)

    def asApiImageTitle(domainImageTitle: domain.ImageTitle): api.ImageTitleDTO = {
      api.ImageTitleDTO(domainImageTitle.title, domainImageTitle.language)
    }

    def asApiLicense(license: String): commonApi.LicenseDTO = {
      getLicense(license)
        .map(l => commonApi.LicenseDTO(l.license.toString, Some(l.description), l.url))
        .getOrElse(commonApi.LicenseDTO("unknown", None, None))
    }

    def asApiUrl(url: String, baseUrl: Option[String] = None): String = {
      val pathToAdd = UrlPath.parse("/" + url.dropWhile(_ == '/'))
      val base      = baseUrl.getOrElse("")
      val basePath  = base.path.addParts(pathToAdd.parts)
      base.withPath(basePath).toString
    }

    def withNewImage(
        imageMeta: ImageMetaInformation,
        image: ImageFileData,
        language: String,
        user: TokenUser
    ): ImageMetaInformation = {
      val now       = clock.now()
      val newNote   = domain.EditorNote(now, user.id, s"Updated image file for '$language' language.")
      val newImages = imageMeta.images.map(_.filterNot(_.language == language) :+ image)
      imageMeta.copy(
        images = newImages,
        editorNotes = imageMeta.editorNotes :+ newNote
      )
    }

    def asDomainImageMetaInformationV2(
        imageMeta: api.NewImageMetaInformationV2DTO,
        user: TokenUser
    ): Try[ImageMetaInformation] = {
      val modelReleasedStatus = imageMeta.modelReleased match {
        case Some(mrs) => ModelReleasedStatus.valueOfOrError(mrs)
        case None      => Success(ModelReleasedStatus.NOT_SET)
      }

      modelReleasedStatus.map(modelStatus => {
        val now = clock.now()

        new ImageMetaInformation(
          id = None,
          titles = Seq(asDomainTitle(imageMeta.title, imageMeta.language)),
          alttexts = imageMeta.alttext.map(at => asDomainAltText(at, imageMeta.language)).toSeq,
          images = None,
          copyright = toDomainCopyright(imageMeta.copyright),
          tags = if (imageMeta.tags.nonEmpty) Seq(toDomainTag(imageMeta.tags, imageMeta.language)) else Seq.empty,
          captions = Seq(domain.ImageCaption(imageMeta.caption, imageMeta.language)),
          updatedBy = user.id,
          createdBy = user.id,
          created = now,
          updated = now,
          modelReleased = modelStatus,
          editorNotes = Seq(domain.EditorNote(now, user.id, "Image created."))
        )
      })
    }

    def asDomainTitle(title: String, language: String): domain.ImageTitle = {
      domain.ImageTitle(title, language)
    }

    def asDomainAltText(alt: String, language: String): domain.ImageAltText = {
      domain.ImageAltText(alt, language)
    }

    def toDomainCopyright(copyright: commonApi.CopyrightDTO): commonDomain.article.Copyright = {
      commonDomain.article.Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(_.toDomain),
        copyright.processors.map(_.toDomain),
        copyright.rightsholders.map(_.toDomain),
        copyright.validFrom,
        copyright.validTo,
        copyright.processed
      )
    }

    def toDomainTag(tags: Seq[String], language: String): commonDomain.Tag = {
      commonDomain.Tag(tags, language)
    }

    def toDomainCaption(caption: String, language: String): domain.ImageCaption = {
      domain.ImageCaption(caption, language)
    }

    def withoutLanguage(
        domainMetaInformation: ImageMetaInformation,
        languageToRemove: String,
        user: TokenUser
    ): ImageMetaInformation = {
      val now     = clock.now()
      val newNote = domain.EditorNote(now, user.id, s"Deleted language '$languageToRemove'.")
      domainMetaInformation.copy(
        titles = domainMetaInformation.titles.filterNot(_.language == languageToRemove),
        alttexts = domainMetaInformation.alttexts.filterNot(_.language == languageToRemove),
        tags = domainMetaInformation.tags.filterNot(_.language == languageToRemove),
        captions = domainMetaInformation.captions.filterNot(_.language == languageToRemove),
        editorNotes = domainMetaInformation.editorNotes :+ newNote,
        images = domainMetaInformation.images.map(_.filterNot(_.language == languageToRemove))
      )
    }

    def getSupportedLanguages(domainImageMetaInformation: ImageMetaInformation): Seq[String] = {
      Language.getSupportedLanguages(
        domainImageMetaInformation.titles,
        domainImageMetaInformation.alttexts,
        domainImageMetaInformation.tags,
        domainImageMetaInformation.captions,
        domainImageMetaInformation.images.getOrElse(Seq.empty)
      )
    }

    def toImageDocument(image: UploadedImage, language: String): ImageFileDataDocument = {
      new ImageFileDataDocument(
        size = image.size,
        contentType = image.contentType,
        dimensions = image.dimensions,
        language = language
      )
    }
  }

}
