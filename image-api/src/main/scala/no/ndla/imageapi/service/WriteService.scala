/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.errors.{MissingIdException, ValidationException}
import no.ndla.common.implicits.*
import no.ndla.common.model.api.{Deletable, Delete, Missing, UpdateWith}
import no.ndla.common.model.domain.UploadedFile
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.imageapi.model.*
import no.ndla.imageapi.model.api.{
  ImageMetaInformationV2DTO,
  ImageMetaInformationV3DTO,
  NewImageMetaInformationV2DTO,
  UpdateImageMetaInformationDTO,
}
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, TagIndexService}
import no.ndla.language.Language
import no.ndla.language.Language.{mergeLanguageFields, sortByLanguagePriority}
import no.ndla.language.model.LanguageField
import no.ndla.network.tapir.auth.TokenUser

import java.io.ByteArrayInputStream
import java.lang.Math.max
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

class WriteService(using
    converterService: ConverterService,
    validationService: ValidationService,
    imageRepository: ImageRepository,
    imageIndexService: ImageIndexService,
    imageStorage: ImageStorageService,
    tagIndexService: TagIndexService,
    clock: Clock,
    random: Random,
) extends StrictLogging {

  def deleteImageLanguageVersionV2(
      imageId: Long,
      language: String,
      user: TokenUser,
  ): Try[Option[ImageMetaInformationV2DTO]] = {
    deleteImageLanguageVersion(imageId, language, user).flatMap {
      case Some(updated) =>
        converterService.asApiImageMetaInformationWithDomainUrlV2(updated, None, user.some).map(_.some)
      case None => Success(None)
    }
  }

  def deleteImageLanguageVersionV3(
      imageId: Long,
      language: String,
      user: TokenUser,
  ): Try[Option[ImageMetaInformationV3DTO]] = {
    deleteImageLanguageVersion(imageId, language, user).flatMap {
      case Some(updated) => converterService.asApiImageMetaInformationV3(updated, None, user.some).map(_.some)
      case None          => Success(None)
    }
  }

  private def deleteFileForLanguageIfUnused(imageId: Long, images: Seq[ImageFileData], language: String): Try[?] = {
    val imageFileToDelete = images.find(_.language == language)
    imageFileToDelete match {
      case Some(fileToDelete) =>
        val deletedMeta           = imageRepository.deleteImageFileMeta(imageId, language)
        val otherLangs            = images.filterNot(_.language == language)
        val imageIsUsedOtherwhere = otherLangs.exists(_.fileName == fileToDelete.fileName)

        if (!imageIsUsedOtherwhere) {
          for {
            _ <- imageStorage.deleteObject(fileToDelete.fileName)
            _ <- deletedMeta
          } yield ()
        } else {
          logger.info("Image is used by other languages. Skipping file delete")
          deletedMeta
        }
      case None =>
        logger.warn("Deleting language for image without imagefile. This is weird.")
        Success(())
    }
  }

  private[service] def deleteImageLanguageVersion(
      imageId: Long,
      language: String,
      user: TokenUser,
  ): Try[Option[ImageMetaInformation]] = permitTry {
    imageRepository.withId(imageId) match {
      case Some(existing) if converterService.getSupportedLanguages(existing).contains(language) =>
        val newImage = converterService.withoutLanguage(existing, language, user)

        // If last language version delete entire image
        val isLastLanguage = converterService.getSupportedLanguages(newImage).isEmpty
        if (isLastLanguage) {
          deleteImageAndFiles(imageId).map(_ => None)
        } else {
          deleteFileForLanguageIfUnused(imageId, existing.images.getOrElse(Seq.empty), language).??
          updateAndIndexImage(imageId, newImage, existing.some).map(_.some)
        }

      case Some(_) =>
        Failure(new ImageNotFoundException(s"Image with id $imageId does not exist in language '$language'."))
      case None =>
        Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
    }
  }

  def deleteImageAndFiles(imageId: Long): Try[Long] = {
    imageRepository.withId(imageId) match {
      case Some(toDelete) =>
        val metaDeleted  = imageRepository.delete(imageId)
        val filesDeleted = toDelete
          .images
          .getOrElse(Seq.empty)
          .traverse(image => {
            imageStorage.deleteObject(image.fileName)
          })
        val indexDeleted = imageIndexService.deleteDocument(imageId).flatMap(tagIndexService.deleteDocument)

        if (metaDeleted < 1) {
          Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
        } else if (filesDeleted.isFailure) {
          Failure(new ImageStorageException("Something went wrong when deleting image file from storage."))
        } else {
          indexDeleted
        }
      case None =>
        Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
    }
  }

  def copyImage(
      imageId: Long,
      newFile: UploadedFile,
      maybeLanguage: Option[String],
      user: TokenUser,
  ): Try[ImageMetaInformation] = {
    imageRepository.withId(imageId) match {
      case None           => Failure(new ImageNotFoundException(s"Image with id $imageId was not found."))
      case Some(existing) =>
        val now       = clock.now()
        val newTitles = existing.titles.map(t => t.copy(title = t.title + " (Kopi)"))
        val toInsert  = existing.copy(
          id = None,
          titles = newTitles,
          images = None,
          editorNotes = Seq(domain.EditorNote(now, user.id, s"Image created as a copy of image with id '$imageId'.")),
        )

        val language = Language
          .findByLanguageOrBestEffort(existing.images.getOrElse(Seq.empty), maybeLanguage)
          .map(_.language)
          .getOrElse(Language.DefaultLanguage)
        insertAndStoreImage(toInsert, newFile, existing.some, language)
    }
  }

  private def insertAndStoreImage(
      toInsert: ImageMetaInformation,
      file: UploadedFile,
      copiedFrom: Option[ImageMetaInformation],
      language: String,
  ): Try[ImageMetaInformation] = permitTry {
    (
      validationService.validateImageFile(file) match {
        case Some(validationMessage) => Failure(new ValidationException(errors = Seq(validationMessage)))
        case _                       => Success(())
      }
    ).?

    validationService.validate(toInsert, copiedFrom).??
    val insertedMeta       = Try(imageRepository.insert(toInsert)).?
    val missingIdException = MissingIdException("Could not find id of stored metadata. This is a bug.")
    val imageId            = insertedMeta.id.toTry(missingIdException).?

    val uploadedImage = uploadImage(file).?

    val imageDocument = converterService.toImageDocument(uploadedImage, language)
    val image         = imageRepository.insertImageFile(imageId, uploadedImage.fileName, imageDocument).?
    val imageMeta     = insertedMeta.copy(images = Some(Seq(image)))

    val deleteUploadedImages = (reason: Throwable) => {
      logger.info(s"Deleting images because of: ${reason.getMessage}", reason)
      imageMeta.images.getOrElse(Seq.empty).traverse(image => imageStorage.deleteObject(image.fileName))
    }

    imageIndexService
      .indexDocument(imageMeta)
      .recoverWith { e =>
        deleteUploadedImages(e): Unit
        Try(imageRepository.delete(imageId)): Unit
        Failure(e)
      }
      .??

    tagIndexService.indexDocument(imageMeta) match {
      case Success(_) => Success(imageMeta)
      case Failure(e) =>
        deleteUploadedImages(e): Unit
        imageIndexService.deleteDocument(imageId): Unit
        tagIndexService.deleteDocument(imageId): Unit
        Try(imageRepository.delete(imageId)): Unit
        Failure(e)
    }
  }

  def storeNewImage(
      newImage: NewImageMetaInformationV2DTO,
      file: UploadedFile,
      user: TokenUser,
  ): Try[ImageMetaInformation] = permitTry {
    val toInsert = converterService.asDomainImageMetaInformationV2(newImage, user).?
    insertAndStoreImage(toInsert, file, None, newImage.language)
  }

  private def hasChangedMetadata(lhs: ImageMetaInformation, rhs: ImageMetaInformation): Boolean = {
    val withoutMetas = (i: ImageMetaInformation) => i.copy(images = None, updated = NDLADate.MIN, updatedBy = "")

    withoutMetas(lhs) != withoutMetas(rhs)
  }

  def mergeDeletableLanguageFields[A <: LanguageField[?]](
      existing: Seq[A],
      updated: Deletable[A],
      language: String,
  ): Seq[A] = (
    updated match {
      case Left(_)               => existing.filterNot(_.language == language)
      case Right(None)           => existing
      case Right(Some(newValue)) => existing.filterNot(_.language == language) :+ newValue
    }
  ).filterNot(_.isEmpty)

  private[service] def mergeImages(
      existing: ImageMetaInformation,
      toMerge: UpdateImageMetaInformationDTO,
      user: TokenUser,
  ): Try[ImageMetaInformation] = {
    val now    = clock.now()
    val userId = user.id

    val alttexts = toMerge.alttext match {
      case Missing           => existing.alttexts
      case Delete            => existing.alttexts.filterNot(_.language == toMerge.language)
      case UpdateWith(value) => existing.alttexts.filterNot(_.language == toMerge.language) :+ converterService
          .asDomainAltText(value, toMerge.language)
    }

    val newImageMeta = existing.copy(
      titles = mergeLanguageFields(
        existing.titles,
        toMerge.title.toSeq.map(t => converterService.asDomainTitle(t, toMerge.language)),
      ),
      alttexts = alttexts,
      copyright = toMerge.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
      tags = mergeTags(existing.tags, toMerge.tags.toSeq.map(t => converterService.toDomainTag(t, toMerge.language))),
      captions = mergeLanguageFields(
        existing.captions,
        toMerge.caption.toSeq.map(c => converterService.toDomainCaption(c, toMerge.language)),
      ),
      updated = now,
      updatedBy = userId,
      modelReleased = toMerge.modelReleased.flatMap(ModelReleasedStatus.valueOf).getOrElse(existing.modelReleased),
    )

    val existingLanguages = converterService.getSupportedLanguages(existing)
    val isNewLanguage     = !existingLanguages.contains(toMerge.language)
    val newEditorNotes    = {
      if (isNewLanguage)
        existing.editorNotes :+ domain.EditorNote(now, userId, s"Added new language '${toMerge.language}'.")
      else if (hasChangedMetadata(existing, newImageMeta))
        existing.editorNotes :+ domain.EditorNote(now, userId, "Updated image data.")
      else existing.editorNotes
    }

    existing
      .images
      .traverse(ims => insertImageCopyIfNoImage(ims, toMerge.language))
      .map(newImages => newImageMeta.copy(images = newImages, editorNotes = newEditorNotes))
  }

  private def insertImageCopyIfNoImage(
      images: Seq[domain.ImageFileData],
      language: String,
  ): Try[Seq[domain.ImageFileData]] = {
    if (images.exists(_.language == language)) {
      Success(images)
    } else {
      sortByLanguagePriority(images).headOption match {
        case Some(imageToCopy) =>
          val document = imageToCopy.toDocument().copy(language = language)
          imageRepository
            .insertImageFile(imageToCopy.imageMetaId, imageToCopy.fileName, document)
            .map(inserted => images :+ inserted)
        case None => Failure(ImageCopyException("Could not find any imagefilemeta when attempting copy."))
      }
    }
  }

  private def mergeTags(existing: Seq[common.Tag], updated: Seq[common.Tag]): Seq[common.Tag] = {
    val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
    (
      toKeep ++ updated
    ).filterNot(_.tags.isEmpty)
  }

  private def updateAndIndexImage(
      imageId: Long,
      image: ImageMetaInformation,
      oldImage: Option[ImageMetaInformation],
  ): Try[ImageMetaInformation] = {
    for {
      validated     <- validationService.validate(image, oldImage)
      updated       <- imageRepository.update(validated, imageId)
      indexed       <- imageIndexService.indexDocument(updated)
      indexedByTags <- tagIndexService.indexDocument(indexed)
    } yield indexedByTags
  }

  private def updateImageFile(
      imageId: Long,
      newFile: UploadedFile,
      oldImage: ImageMetaInformation,
      language: String,
      user: TokenUser,
  ): Try[ImageMetaInformation] = permitTry {
    val imageForLang  = oldImage.images.getOrElse(Seq.empty).find(_.language == language)
    val allOtherPaths = oldImage.images.getOrElse(Seq.empty).filterNot(_.language == language).map(_.fileName)

    val uploaded = uploadImage(newFile).?

    val imageFileFrom = (existingImageFileMeta: ImageFileData) => {
      domain.ImageFileData(
        id = existingImageFileMeta.id,
        fileName = uploaded.fileName,
        size = uploaded.size,
        contentType = uploaded.contentType,
        dimensions = uploaded.dimensions,
        language = existingImageFileMeta.language,
        imageMetaId = existingImageFileMeta.imageMetaId,
      )
    }

    val imageFileData = (
      imageForLang match {
        case Some(existingImage) if !allOtherPaths.contains(existingImage.fileName) =>
          // Put new image file at old path if no other languages use it
          val clonedImage = imageFileFrom(existingImage).copy(fileName = existingImage.fileName)
          imageStorage.cloneObject(uploaded.fileName, existingImage.fileName).?
          Success(clonedImage)
        case Some(existingImage) => Success(imageFileFrom(existingImage))
        case None                =>
          val doc = converterService.toImageDocument(uploaded, language)
          imageRepository.insertImageFile(imageId, uploaded.fileName, doc)
      }
    ).?

    val withNew = converterService.withNewImage(oldImage, imageFileData, language, user)
    Success(withNew)
  }

  private[service] def updateImageAndFile(
      imageId: Long,
      updateMeta: UpdateImageMetaInformationDTO,
      newFile: Option[UploadedFile],
      user: TokenUser,
  ): Try[domain.ImageMetaInformation] = {
    imageRepository.withId(imageId) match {
      case None           => Failure(new ImageNotFoundException(s"Image with id $imageId found"))
      case Some(oldImage) =>
        val maybeOverwrittenImage = newFile match {
          case Some(file) => validationService.validateImageFile(file) match {
              case Some(validationMessage) => Failure(new ValidationException(errors = Seq(validationMessage)))
              case _                       => updateImageFile(imageId, file, oldImage, updateMeta.language, user)
            }
          case _ => Success(oldImage)
        }

        for {
          overwritten <- maybeOverwrittenImage
          newImage    <- mergeImages(overwritten, updateMeta, user)
          indexed     <- updateAndIndexImage(imageId, newImage, oldImage.some)
        } yield indexed
    }
  }

  def updateImage(
      imageId: Long,
      updateMeta: UpdateImageMetaInformationDTO,
      newFile: Option[UploadedFile],
      user: TokenUser,
  ): Try[ImageMetaInformationV2DTO] = for {
    updated   <- updateImageAndFile(imageId, updateMeta, newFile, user)
    converted <- converterService.asApiImageMetaInformationWithDomainUrlV2(updated, updateMeta.language.some, user.some)
  } yield converted

  def updateImageV3(
      imageId: Long,
      updateMeta: UpdateImageMetaInformationDTO,
      newFile: Option[UploadedFile],
      user: TokenUser,
  ): Try[ImageMetaInformationV3DTO] = for {
    updated   <- updateImageAndFile(imageId, updateMeta, newFile, user)
    converted <- converterService.asApiImageMetaInformationV3(updated, updateMeta.language.some, user.some)
  } yield converted

  private[service] def getFileExtension(fileName: String): Option[String] = {
    fileName.lastIndexOf(".") match {
      case index: Int if index > -1 => Some(fileName.substring(index))
      case _                        => None
    }
  }

  private def uploadImageWithName(file: UploadedFile, fileName: String): Try[UploadedImage] = {
    val contentType = file.contentType.getOrElse("")
    val bytes       = file.stream.readAllBytes()
    val image       = Try(Option(ImageIO.read(new ByteArrayInputStream(bytes))))

    val dimensions = image match {
      case Failure(ex) =>
        logger.error("Something went wrong when getting imageDimensions", ex)
        Some(ImageDimensions(0, 0))
      case Success(Some(image)) => Some(ImageDimensions(image.getWidth, image.getHeight))
      case Success(None)        =>
        val isSVG = new String(bytes).toLowerCase.contains("<svg")
        // Since SVG are vector-images size doesn't make sense
        if (isSVG) None
        else {
          logger.error("Something _weird_ went wrong when getting imageDimensions")
          Some(ImageDimensions(0, 0))
        }
    }

    imageStorage
      .uploadFromStream(fileName, file)
      .map(filePath => {
        UploadedImage(filePath, file.fileSize, contentType, dimensions)
      })
  }

  private[service] def uploadImage(file: UploadedFile): Try[UploadedImage] = {
    val extension = file.fileName.flatMap(getFileExtension).getOrElse("")
    val fileName  = LazyList.continually(randomFileName(extension)).dropWhile(imageStorage.objectExists).head
    uploadImageWithName(file, fileName)
  }

  private[service] def randomFileName(extension: String, length: Int = 12): String = {
    val extensionWithDot =
      if (extension.headOption.contains('.')) extension
      else if (extension.nonEmpty) s".$extension"
      else ""

    val randomLength = max(length - extensionWithDot.length, 1)
    val randomString = random.string(randomLength)
    s"$randomString$extensionWithDot"
  }
}
