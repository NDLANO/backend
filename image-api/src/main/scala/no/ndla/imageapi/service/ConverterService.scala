/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.typesafe.dsl._
import io.lemonlabs.uri.{Uri, UrlPath}
import no.ndla.imageapi.Props
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.domain.{ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.model.{ImageConversionException, ImageStorageException, api, domain}
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl
import cats.implicits._

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: User with Role with Clock with DraftApiClient with Props =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    import props.DefaultLanguage

    def asApiAuthor(domainAuthor: domain.Author): api.Author = {
      api.Author(domainAuthor.`type`, domainAuthor.name)
    }

    def asApiCopyright(domainCopyright: domain.Copyright): api.Copyright = {
      api.Copyright(
        asApiLicense(domainCopyright.license),
        domainCopyright.origin,
        domainCopyright.creators.map(asApiAuthor),
        domainCopyright.processors.map(asApiAuthor),
        domainCopyright.rightsholders.map(asApiAuthor),
        domainCopyright.agreementId,
        domainCopyright.validFrom,
        domainCopyright.validTo
      )
    }

    def asApiImage(domainImage: domain.Image, baseUrl: Option[String] = None): api.Image = {
      api.Image(baseUrl.getOrElse("") + domainImage.fileName, domainImage.size, domainImage.contentType)
    }

    def asApiImageAltText(domainImageAltText: domain.ImageAltText): api.ImageAltText = {
      api.ImageAltText(domainImageAltText.alttext, domainImageAltText.language)
    }

    def asApiImageMetaInformationWithApplicationUrlV2(
        domainImageMetaInformation: domain.ImageMetaInformation,
        language: Option[String]
    ): Try[api.ImageMetaInformationV2] = {
      val baseUrl = ApplicationUrl.get
      val rawPath = baseUrl.replace("/v2/images/", "/raw")
      asImageMetaInformationV2(domainImageMetaInformation, language, ApplicationUrl.get, Some(rawPath))
    }

    def asApiImageMetaInformationWithDomainUrlV2(
        domainImageMetaInformation: domain.ImageMetaInformation,
        language: Option[String]
    ): Try[api.ImageMetaInformationV2] = {
      asImageMetaInformationV2(
        domainImageMetaInformation,
        language,
        props.ImageApiUrlBase,
        Some(props.RawImageUrlBase)
      )
    }

    private def asApiEditorNotes(notes: Seq[domain.EditorNote]): Seq[api.EditorNote] = {
      notes.map(n => api.EditorNote(n.timeStamp, n.updatedBy, n.note))
    }

    private def getImageFromMeta(meta: domain.ImageMetaInformation, language: Option[String]): Try[domain.Image] = {
      findByLanguageOrBestEffort(meta.images, language) match {
        case None        => Failure(ImageConversionException("Could not find image in meta, this is a bug."))
        case Some(image) => Success(image)
      }
    }

    private[service] def asImageMetaInformationV2(
        imageMeta: domain.ImageMetaInformation,
        language: Option[String],
        baseUrl: String,
        rawBaseUrl: Option[String]
    ): Try[api.ImageMetaInformationV2] = {
      val title = findByLanguageOrBestEffort(imageMeta.titles, language)
        .map(asApiImageTitle)
        .getOrElse(api.ImageTitle("", DefaultLanguage))
      val alttext = findByLanguageOrBestEffort(imageMeta.alttexts, language)
        .map(asApiImageAltText)
        .getOrElse(api.ImageAltText("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(imageMeta.tags, language)
        .map(asApiImageTag)
        .getOrElse(api.ImageTag(Seq(), DefaultLanguage))
      val caption = findByLanguageOrBestEffort(imageMeta.captions, language)
        .map(asApiCaption)
        .getOrElse(api.ImageCaption("", DefaultLanguage))

      getImageFromMeta(imageMeta, language).flatMap(image => {
        val apiUrl          = asApiUrl(image.fileName, rawBaseUrl)
        val editorNotes     = Option.when(authRole.userHasWriteRole())(asApiEditorNotes(imageMeta.editorNotes))
        val imageDimensions = image.dimensions.map(d => api.ImageDimensions(d.width, d.height))

        Success(
          api
            .ImageMetaInformationV2(
              id = imageMeta.id.get.toString,
              metaUrl = baseUrl + imageMeta.id.get,
              title = title,
              alttext = alttext,
              imageUrl = apiUrl,
              size = image.size,
              contentType = image.contentType,
              copyright = withAgreementCopyright(asApiCopyright(imageMeta.copyright)),
              tags = tags,
              caption = caption,
              supportedLanguages = getSupportedLanguages(imageMeta),
              created = imageMeta.created,
              createdBy = imageMeta.createdBy,
              modelRelease = imageMeta.modelReleased.toString,
              editorNotes = editorNotes,
              imageDimensions = imageDimensions
            )
        )
      })
    }

    def withAgreementCopyright(image: domain.ImageMetaInformation): domain.ImageMetaInformation = {
      val agreementCopyright = image.copyright.agreementId
        .flatMap(aid => draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright))
        .getOrElse(image.copyright)

      image.copy(
        copyright = image.copyright.copy(
          license = agreementCopyright.license,
          creators = agreementCopyright.creators,
          rightsholders = agreementCopyright.rightsholders,
          validFrom = agreementCopyright.validFrom,
          validTo = agreementCopyright.validTo
        )
      )
    }

    def withAgreementCopyright(copyright: api.Copyright): api.Copyright = {
      val agreementCopyright =
        copyright.agreementId.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(copyright)
      copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      )
    }

    def asApiImageTag(domainImageTag: domain.ImageTag): api.ImageTag = {
      api.ImageTag(domainImageTag.tags, domainImageTag.language)
    }

    def asApiCaption(domainImageCaption: domain.ImageCaption): api.ImageCaption =
      api.ImageCaption(domainImageCaption.caption, domainImageCaption.language)

    def asApiImageTitle(domainImageTitle: domain.ImageTitle): api.ImageTitle = {
      api.ImageTitle(domainImageTitle.title, domainImageTitle.language)
    }

    def asApiLicense(license: String): api.License = {
      getLicense(license)
        .map(l => api.License(l.license.toString, l.description, l.url))
        .getOrElse(api.License("unknown", "", None))
    }

    def asApiUrl(url: String, baseUrl: Option[String] = None): String = {
      val pathToAdd = UrlPath.parse("/" + url.dropWhile(_ == '/'))
      val base      = baseUrl.getOrElse("")
      val basePath  = base.path.addParts(pathToAdd.parts)
      base.withPath(basePath).toString
    }

    def withNewImage(
        imageMeta: domain.ImageMetaInformation,
        image: domain.Image,
        language: String
    ): ImageMetaInformation = {
      val user      = authUser.userOrClientid()
      val now       = clock.now()
      val newNote   = domain.EditorNote(now, user, s"Updated image file for '$language' language.")
      val newImages = imageMeta.images.filterNot(_.language == language) :+ image
      imageMeta.copy(
        images = newImages,
        editorNotes = imageMeta.editorNotes :+ newNote
      )
    }

    def asDomainImageMetaInformationV2(
        imageMeta: api.NewImageMetaInformationV2,
        image: domain.Image
    ): Try[domain.ImageMetaInformation] = {
      val modelReleasedStatus = imageMeta.modelReleased match {
        case Some(mrs) => ModelReleasedStatus.valueOfOrError(mrs)
        case None      => Success(ModelReleasedStatus.NOT_SET)
      }

      modelReleasedStatus.map(modelStatus => {
        val now  = clock.now()
        val user = authUser.userOrClientid()

        domain.ImageMetaInformation(
          id = None,
          titles = Seq(asDomainTitle(imageMeta.title, imageMeta.language)),
          alttexts = Seq(asDomainAltText(imageMeta.alttext, imageMeta.language)),
          images = Seq(image),
          copyright = toDomainCopyright(imageMeta.copyright),
          tags = if (imageMeta.tags.nonEmpty) Seq(toDomainTag(imageMeta.tags, imageMeta.language)) else Seq.empty,
          captions = Seq(domain.ImageCaption(imageMeta.caption, imageMeta.language)),
          updatedBy = user,
          createdBy = user,
          created = now,
          updated = now,
          modelReleased = modelStatus,
          editorNotes = Seq(domain.EditorNote(now, user, "Image created."))
        )
      })
    }

    def asDomainTitle(title: String, language: String): domain.ImageTitle = {
      domain.ImageTitle(title, language)
    }

    def asDomainAltText(alt: String, language: String): domain.ImageAltText = {
      domain.ImageAltText(alt, language)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toDomainAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def toDomainTag(tags: Seq[String], language: String): domain.ImageTag = {
      domain.ImageTag(tags, language)
    }

    def toDomainCaption(caption: String, language: String): domain.ImageCaption = {
      domain.ImageCaption(caption, language)
    }

    def withoutLanguage(
        domainMetaInformation: domain.ImageMetaInformation,
        languageToRemove: String
    ): domain.ImageMetaInformation = {
      val now    = clock.now()
      val userId = authUser.userOrClientid()
      domainMetaInformation.copy(
        titles = domainMetaInformation.titles.filterNot(_.language == languageToRemove),
        alttexts = domainMetaInformation.alttexts.filterNot(_.language == languageToRemove),
        tags = domainMetaInformation.tags.filterNot(_.language == languageToRemove),
        captions = domainMetaInformation.captions.filterNot(_.language == languageToRemove),
        editorNotes =
          domainMetaInformation.editorNotes :+ domain.EditorNote(now, userId, s"Deleted language '$languageToRemove'.")
      )
    }

    def getSupportedLanguages(domainImageMetaInformation: domain.ImageMetaInformation): Seq[String] = {
      Language.getSupportedLanguages(
        domainImageMetaInformation.titles,
        domainImageMetaInformation.alttexts,
        domainImageMetaInformation.tags,
        domainImageMetaInformation.captions
      )
    }

  }

}
