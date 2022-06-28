/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model._
import no.ndla.imageapi.model.api.{
  ImageMetaInformationV2,
  ImageMetaInformationV3,
  NewImageMetaInformationV2,
  UpdateImageMetaInformation
}
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, TagIndexService}
import no.ndla.language.Language.mergeLanguageFields
import no.ndla.scalatra.error.ValidationException
import org.scalatra.servlet.FileItem

import java.io.ByteArrayInputStream
import java.lang.Math.max
import java.util.Date
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ConverterService
    with ValidationService
    with ImageRepository
    with ImageIndexService
    with ImageStorageService
    with TagIndexService
    with Clock
    with User
    with Props
    with DBImageFile
    with DBImageMetaInformation
    with Random =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def deleteImageLanguageVersionV2(imageId: Long, language: String): Try[Option[ImageMetaInformationV2]] = {
      deleteImageLanguageVersion(imageId, language).flatMap {
        case Some(updated) => converterService.asApiImageMetaInformationWithDomainUrlV2(updated, None).map(_.some)
        case None          => Success(None)
      }
    }

    def deleteImageLanguageVersionV3(imageId: Long, language: String): Try[Option[ImageMetaInformationV3]] = {
      deleteImageLanguageVersion(imageId, language).flatMap {
        case Some(updated) => converterService.asApiImageMetaInformationV3(updated, None).map(_.some)
        case None          => Success(None)
      }
    }
    private def deleteFileForLanguageIfUnused(images: Seq[ImageFileData], language: String): Try[_] = {
      val imageFileToDelete = images.find(_.language == language)
      imageFileToDelete match {
        case Some(fileToDelete) =>
          val imageIsUsedOtherwhere =
            images.filterNot(_.language == language).exists(_.fileName == fileToDelete.fileName)
          if (!imageIsUsedOtherwhere) {
            imageStorage.deleteObject(fileToDelete.fileName)
          } else {
            logger.info("Image is used by other languages. Skipping file delete")
            Success(())
          }
        case None =>
          logger.warn("Deleting language for image without imagefile. This is weird.")
          Success(())
        case _ => Success(())
      }
    }

    private[service] def deleteImageLanguageVersion(
        imageId: Long,
        language: String
    ): Try[Option[ImageMetaInformation]] =
      imageRepository.withId(imageId) match {
        case Some(existing) if converterService.getSupportedLanguages(existing).contains(language) =>
          val newImage = converterService.withoutLanguage(existing, language)

          // If last language version delete entire image
          val isLastLanguage = converterService.getSupportedLanguages(newImage).isEmpty
          if (isLastLanguage) {
            deleteImageAndFiles(imageId).map(_ => None)
          } else {
            deleteFileForLanguageIfUnused(existing.images, language).flatMap(_ =>
              updateAndIndexImage(imageId, newImage, existing.some).map(_.some)
            )
          }

        case Some(_) =>
          Failure(new ImageNotFoundException(s"Image with id $imageId does not exist in language '$language'."))
        case None =>
          Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
      }

    def deleteImageAndFiles(imageId: Long): Try[Long] = {
      imageRepository.withId(imageId) match {
        case Some(toDelete) =>
          val metaDeleted  = imageRepository.delete(imageId)
          val filesDeleted = toDelete.images.traverse(image => imageStorage.deleteObject(image.fileName))
          val indexDeleted = imageIndexService.deleteDocument(imageId).flatMap(tagIndexService.deleteDocument)

          if (metaDeleted < 1) {
            Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
          } else if (filesDeleted.isFailure) {
            Failure(new ImageStorageException("Something went wrong when deleting image file from storage."))
          } else {
            indexDeleted match {
              case Success(deleteId) => Success(deleteId)
              case Failure(ex)       => Failure(ex)
            }
          }
        case None =>
          Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
      }
    }

    def storeNewImage(newImage: NewImageMetaInformationV2, file: FileItem): Try[ImageMetaInformation] = {
      validationService.validateImageFile(file) match {
        case Some(validationMessage) => return Failure(new ValidationException(errors = Seq(validationMessage)))
        case _                       =>
      }

      val toInsert = converterService.asDomainImageMetaInformationV2(newImage) match {
        case Failure(ex)        => return Failure(ex)
        case Success(converted) => converted
      }

      validationService.validate(toInsert, None) match {
        case Failure(e) => return Failure(e)
        case _          =>
      }

      val insertedMeta = Try(imageRepository.insert(toInsert)) match {
        case Success(meta) => meta
        case Failure(e)    => return Failure(e)
      }

      val imageId = insertedMeta.id match {
        case Some(id) => id
        case None     => return Failure(MissingIdException("Could not find id of stored metadata. This is a bug."))
      }

      val imageMeta = uploadImage(imageId, file, newImage.language) match {
        case Failure(e) => return Failure(e)
        case Success(image) =>
          insertedMeta.copy(
            images = Seq(image)
          )
      }

      val deleteUploadedImages = (reason: Throwable) => {
        logger.info(s"Deleting images because of: ${reason.getMessage}", reason)
        imageMeta.images.traverse(image => imageStorage.deleteObject(image.fileName))
      }

      imageIndexService.indexDocument(imageMeta) match {
        case Success(_) =>
        case Failure(e) =>
          deleteUploadedImages(e)
          imageRepository.delete(imageMeta.id.get)
          return Failure(e)
      }

      tagIndexService.indexDocument(imageMeta) match {
        case Success(_) => Success(imageMeta)
        case Failure(e) =>
          deleteUploadedImages(e)
          imageIndexService.deleteDocument(imageMeta.id.get)
          tagIndexService.deleteDocument(imageMeta.id.get)
          imageRepository.delete(imageMeta.id.get)
          Failure(e)
      }
    }

    private def hasChangedMetadata(lhs: ImageMetaInformation, rhs: ImageMetaInformation): Boolean = {
      val withoutMetas = (i: ImageMetaInformation) =>
        i.copy(
          images = Seq.empty,
          updated = new Date(0),
          updatedBy = ""
        )

      withoutMetas(lhs) != withoutMetas(rhs)
    }

    private[service] def mergeImages(
        existing: ImageMetaInformation,
        toMerge: UpdateImageMetaInformation
    ): ImageMetaInformation = {
      val now    = clock.now()
      val userId = authUser.userOrClientid()

      val newImageMeta = existing.copy(
        titles = mergeLanguageFields(
          existing.titles,
          toMerge.title.toSeq.map(t => converterService.asDomainTitle(t, toMerge.language))
        ),
        alttexts = mergeLanguageFields(
          existing.alttexts,
          toMerge.alttext.toSeq.map(a => converterService.asDomainAltText(a, toMerge.language))
        ),
        copyright = toMerge.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
        tags = mergeTags(existing.tags, toMerge.tags.toSeq.map(t => converterService.toDomainTag(t, toMerge.language))),
        captions = mergeLanguageFields(
          existing.captions,
          toMerge.caption.toSeq.map(c => converterService.toDomainCaption(c, toMerge.language))
        ),
        updated = now,
        updatedBy = userId,
        modelReleased = toMerge.modelReleased.flatMap(ModelReleasedStatus.valueOf).getOrElse(existing.modelReleased)
      )

      val existingLanguages = converterService.getSupportedLanguages(existing)
      val isNewLanguage     = !existingLanguages.contains(toMerge.language)
      val newEditorNotes = {
        if (isNewLanguage)
          existing.editorNotes :+ domain.EditorNote(now, userId, s"Added new language '${toMerge.language}'.")
        else if (hasChangedMetadata(existing, newImageMeta))
          existing.editorNotes :+ domain.EditorNote(now, userId, "Updated image data.")
        else existing.editorNotes
      }

      newImageMeta.copy(editorNotes = newEditorNotes)
    }

    private def mergeTags(existing: Seq[domain.ImageTag], updated: Seq[domain.ImageTag]): Seq[domain.ImageTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    private def updateAndIndexImage(
        imageId: Long,
        image: ImageMetaInformation,
        oldImage: Option[ImageMetaInformation]
    ): Try[ImageMetaInformation] = {
      for {
        validated <- validationService.validate(image, oldImage)
        updated = imageRepository.update(validated, imageId)
        indexed       <- imageIndexService.indexDocument(updated)
        indexedByTags <- tagIndexService.indexDocument(indexed)
      } yield indexedByTags
    }

    private def updateImageFile(
        imageId: Long,
        newFile: FileItem,
        oldImage: ImageMetaInformation,
        language: String
    ): Try[ImageMetaInformation] =
      uploadImage(imageId, newFile, language).flatMap(uploadedImage => {
        val imageForLang  = oldImage.images.find(_.language == language)
        val allOtherPaths = oldImage.images.filterNot(_.language == language).map(_.fileName)
        imageForLang match {
          case Some(existingImage) if !allOtherPaths.contains(existingImage.fileName) =>
            val clonedImage = uploadedImage.copy(fileName = existingImage.fileName)
            imageStorage
              .cloneObject(uploadedImage.fileName, existingImage.fileName)
              .map(_ => converterService.withNewImage(oldImage, clonedImage, language))
          case _ => Success(converterService.withNewImage(oldImage, uploadedImage, language))
        }
      })

    private[service] def updateImageAndFile(
        imageId: Long,
        updateMeta: UpdateImageMetaInformation,
        newFile: Option[FileItem]
    ): Try[domain.ImageMetaInformation] = {
      imageRepository.withId(imageId) match {
        case None => Failure(new ImageNotFoundException(s"Image with id $imageId found"))
        case Some(oldImage) =>
          val maybeOverwrittenImage = newFile match {
            case Some(file) =>
              validationService.validateImageFile(file) match {
                case Some(validationMessage) => Failure(new ValidationException(errors = Seq(validationMessage)))
                case _                       => updateImageFile(imageId, file, oldImage, updateMeta.language)
              }
            case _ => Success(oldImage)
          }

          maybeOverwrittenImage.flatMap(moi => {
            val newImage = mergeImages(moi, updateMeta)
            updateAndIndexImage(imageId, newImage, oldImage.some)
          })
      }
    }

    def updateImage(
        imageId: Long,
        updateMeta: UpdateImageMetaInformation,
        newFile: Option[FileItem]
    ): Try[ImageMetaInformationV2] =
      for {
        updated   <- updateImageAndFile(imageId, updateMeta, newFile)
        converted <- converterService.asApiImageMetaInformationWithDomainUrlV2(updated, updateMeta.language.some)
      } yield converted

    private[service] def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.substring(index))
        case _                        => None
      }
    }

    private def uploadImageWithName(file: FileItem, fileName: String): Try[UploadedImage] = {
      val contentType = file.getContentType.getOrElse("")
      val bytes       = file.get()
      val image       = Try(Option(ImageIO.read(new ByteArrayInputStream(bytes))))

      val dimensions = image match {
        case Failure(ex) =>
          logger.error("Something went wrong when getting imageDimensions", ex)
          Some(ImageDimensions(0, 0))
        case Success(Some(image)) => Some(ImageDimensions(image.getWidth, image.getHeight))
        case Success(None) =>
          val isSVG = new String(bytes).toLowerCase.contains("<svg")
          // Since SVG are vector-images size doesn't make sense
          if (isSVG) None
          else {
            logger.error("Something _weird_ went wrong when getting imageDimensions")
            Some(ImageDimensions(0, 0))
          }
      }

      imageStorage
        .uploadFromStream(new ByteArrayInputStream(bytes), fileName, contentType, file.size)
        .map(filePath => UploadedImage(filePath, file.size, contentType, dimensions))
    }

    private def uploadAndInsertImage(
        imageId: Long,
        file: FileItem,
        fileName: String,
        language: String
    ): Try[ImageFileData] = {
      uploadImageWithName(file, fileName).flatMap(s3Uploaded => {
        val imageDocument = converterService.toImageDocument(s3Uploaded, language)
        imageRepository.insertImageFile(imageId, s3Uploaded.fileName, imageDocument)
      })
    }

    private[service] def uploadImage(imageId: Long, file: FileItem, language: String): Try[ImageFileData] = {
      val extension = getFileExtension(file.name).getOrElse("")
      val fileName  = LazyList.continually(randomFileName(extension)).dropWhile(imageStorage.objectExists).head
      uploadAndInsertImage(imageId, file, fileName, language)
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

}
