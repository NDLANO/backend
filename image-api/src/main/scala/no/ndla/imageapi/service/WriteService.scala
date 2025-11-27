/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits.*
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.errors.{MissingBucketKeyException, MissingIdException, ValidationException}
import no.ndla.common.implicits.*
import no.ndla.common.model.api.{Deletable, Delete, Missing, UpdateWith}
import no.ndla.common.model.domain.UploadedFile
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.database.DBUtility
import no.ndla.imageapi.model.*
import no.ndla.imageapi.model.api.{
  ImageMetaInformationV2DTO,
  ImageMetaInformationV3DTO,
  NewImageMetaInformationV2DTO,
  UpdateImageMetaInformationDTO,
}
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.WriteService.getWriterForFormat
import no.ndla.imageapi.service.search.{ImageIndexService, TagIndexService}
import no.ndla.language.Language
import no.ndla.language.Language.{mergeLanguageFields, sortByLanguagePriority}
import no.ndla.language.model.LanguageField
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.DBSession

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try, Using}

class WriteService(using
    converterService: ConverterService,
    validationService: ValidationService,
    dbUtility: DBUtility, // TODO: Remove this after completing variants migration of existing images
    imageRepository: ImageRepository,
    imageIndexService: ImageIndexService,
    imageStorage: ImageStorageService,
    imageConverter: ImageConverter,
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

  private def deleteFileForLanguageIfUnused(imageId: Long, images: Seq[ImageFileData], language: String): Try[Unit] = {
    val imageFileToDelete = images.find(_.language == language)
    val otherLangs        = images.filterNot(_.language == language)
    imageFileToDelete match {
      case Some(fileToDelete) if !otherLangs.exists(_.fileName == fileToDelete.fileName) =>
        deleteImageAndVariants(fileToDelete)
      case Some(_) =>
        logger.info("Image is used by other languages. Skipping file delete")
        Success(())
      case None =>
        logger.warn(
          s"Deleting language for image without imagefile. This is weird. [imageId = $imageId, language = $language]"
        )
        Success(())
    }
  }

  private[service] def deleteImageLanguageVersion(
      imageId: Long,
      language: String,
      user: TokenUser,
  ): Try[Option[ImageMetaInformation]] = permitTry {
    imageRepository.withId(imageId).? match {
      case Some(existing) if converterService.getSupportedLanguages(existing).contains(language) =>
        val newImage = converterService.withoutLanguage(existing, language, user)

        // If last language version delete entire image
        val isLastLanguage = converterService.getSupportedLanguages(newImage).isEmpty
        if (isLastLanguage) {
          deleteImageAndFiles(imageId).map(_ => None)
        } else {
          deleteFileForLanguageIfUnused(imageId, existing.images, language).??
          updateAndIndexImage(imageId, newImage, existing.some).map(_.some)
        }

      case Some(_) => Failure(ImageNotFoundException(s"Image with id $imageId does not exist in language '$language'."))
      case None    => Failure(ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
    }
  }

  def deleteImageAndFiles(imageId: Long): Try[Long] = for {
    maybeImageToDelete <- imageRepository.withId(imageId)
    toDelete           <- maybeImageToDelete.toTry(
      ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted.")
    )
    metaDeleted  <- imageRepository.delete(imageId)
    filesDeleted <- toDelete.images.traverse(image => deleteImageAndVariants(image))
    indexDeleted <- imageIndexService.deleteDocument(imageId).flatMap(tagIndexService.deleteDocument)
  } yield indexDeleted

  def copyImage(
      imageId: Long,
      newFile: UploadedFile,
      maybeLanguage: Option[String],
      user: TokenUser,
  ): Try[ImageMetaInformation] = {
    imageRepository.withId(imageId) match {
      case Success(Some(existing)) =>
        val now       = clock.now()
        val newTitles = existing.titles.map(t => t.copy(title = t.title + " (Kopi)"))
        val toInsert  = existing.copy(
          id = None,
          titles = newTitles,
          images = Seq.empty,
          editorNotes = Seq(EditorNote(now, user.id, s"Image created as a copy of image with id '$imageId'.")),
        )

        val language = Language
          .findByLanguageOrBestEffort(existing.images, maybeLanguage)
          .map(_.language)
          .getOrElse(Language.DefaultLanguage)
        insertAndStoreImage(toInsert, newFile, existing.some, language)
      case Success(None) => Failure(new ImageNotFoundException(s"Image with id $imageId was not found."))
      case Failure(ex)   => Failure(ex)
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
    val insertedMeta       = imageRepository.insert(toInsert).?
    val missingIdException = MissingIdException("Could not find id of stored metadata. This is a bug.")
    val imageId            = insertedMeta.id.toTry(missingIdException).?

    val uploadedImage = uploadImageWithVariants(file).?

    val imageFile = converterService.toImageFileData(uploadedImage, language)
    val imageMeta = insertedMeta.copy(images = Seq(imageFile))

    val deleteUploadedImages = (reason: Throwable) => {
      logger.info(s"Deleting images because of: ${reason.getMessage}", reason)
      imageMeta.images.traverse(image => deleteImageAndVariants(image)) match {
        case Success(_)  => ()
        case Failure(ex) => logger.error("Failed to clean up image after failed indexing", ex)
      }
    }

    imageIndexService
      .indexDocument(imageMeta)
      .recoverWith { e =>
        deleteUploadedImages(e)
        Try(imageRepository.delete(imageId)): Unit
        Failure(e)
      }
      .??

    tagIndexService.indexDocument(imageMeta) match {
      case Success(_) => Success(imageMeta)
      case Failure(e) =>
        deleteUploadedImages(e)
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
    val withoutMetas = (i: ImageMetaInformation) => i.copy(images = Seq.empty, updated = NDLADate.MIN, updatedBy = "")

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

  private[service] def mergeImageMeta(
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
      if (isNewLanguage) existing.editorNotes :+ EditorNote(now, userId, s"Added new language '${toMerge.language}'.")
      else if (hasChangedMetadata(existing, newImageMeta))
        existing.editorNotes :+ EditorNote(now, userId, "Updated image data.")
      else existing.editorNotes
    }

    insertImageCopyIfNoImage(existing.images, toMerge.language).map(newImages =>
      newImageMeta.copy(images = newImages, editorNotes = newEditorNotes)
    )
  }

  private def insertImageCopyIfNoImage(
      images: Seq[domain.ImageFileData],
      language: String,
  ): Try[Seq[domain.ImageFileData]] = {
    if (images.exists(_.language == language)) {
      Success(images)
    } else {
      sortByLanguagePriority(images).headOption match {
        case Some(imageToCopy) => Success(images :+ imageToCopy.copy(language = language))
        case None              => Failure(ImageCopyException("Could not find any imagefilemeta when attempting copy."))
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
      newFile: UploadedFile,
      oldImage: ImageMetaInformation,
      language: String,
      user: TokenUser,
  ): Try[ImageMetaInformation] = permitTry {
    val uploaded            = uploadImageWithVariants(newFile).?
    val imageFileFromUpload = converterService.toImageFileData(uploaded, language)

    val imageForLang  = oldImage.images.find(_.language == language)
    val allOtherPaths = oldImage.images.filterNot(_.language == language).map(_.fileName)
    val newImageFile  = imageForLang match {
      case Some(existingImage) if !allOtherPaths.contains(existingImage.fileName) =>
        // Put new image file at old path if no other languages use it
        val movedImage = moveImageAndVariants(imageFileFromUpload, existingImage.getFileStem).?
        movedImage
      case _ => imageFileFromUpload
    }

    val withNew = converterService.withNewImageFile(oldImage, newImageFile, language, user)
    Success(withNew)
  }

  private[service] def updateImageAndFile(
      imageId: Long,
      updateMeta: UpdateImageMetaInformationDTO,
      newFile: Option[UploadedFile],
      user: TokenUser,
  ): Try[ImageMetaInformation] = {
    imageRepository.withId(imageId) match {
      case Success(Some(oldImage)) =>
        val maybeOverwrittenImage = newFile match {
          case Some(file) => validationService.validateImageFile(file) match {
              case Some(validationMessage) => Failure(new ValidationException(errors = Seq(validationMessage)))
              case _                       => updateImageFile(file, oldImage, updateMeta.language, user)
            }
          case _ => Success(oldImage)
        }

        for {
          overwritten <- maybeOverwrittenImage
          newImage    <- mergeImageMeta(overwritten, updateMeta, user)
          indexed     <- updateAndIndexImage(imageId, newImage, oldImage.some)
        } yield indexed
      case Success(None) => Failure(new ImageNotFoundException(s"Image with id $imageId found"))
      case Failure(ex)   => Failure(ex)
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

  // TODO: Remove this after completing variants migration of existing images
  def generateAndUploadVariantsForExistingImages(ignoreMissingObjects: Boolean): Try[Unit] = {
    val processableContentTypes = Seq("image/png", "image/jpeg")
    val batchSize               = 20
    val batchIterator           = imageRepository.getImageFileBatched(batchSize) match {
      case Success(it) => it
      case Failure(ex) => return Failure(ex)
    }
    val totalBatchCount    = batchIterator.knownSize
    given ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(batchSize))

    batchIterator
      .zipWithIndex
      .map { (batch, index) =>
        logger.info(s"Processing batch ${index + 1} of $totalBatchCount (batch size = $batchSize)")

        val batchFuture = Future.traverse {
          batch
            .mapFilter { imageMeta =>
              Option.when(imageMeta.id.nonEmpty && imageMeta.images.exists(_.variants.isEmpty))(imageMeta)
            }
            .flatMap(imageMeta => imageMeta.images.tupleLeft(imageMeta))
        } { (imageMeta, imageFile) =>
          if (processableContentTypes.contains(imageFile.contentType)) {
            generateAndUploadVariantsForImageFileDataAsync(ignoreMissingObjects)(imageMeta, imageFile)
          } else {
            Future.successful(Success(imageMeta -> imageFile))
          }
        }

        val storeResultsFuture = batchFuture.map { metasWithFilesT =>
          metasWithFilesT
            .sequence
            .flatMap { metasWithFiles =>
              metasWithFiles
                .groupMap((meta, _) => meta)((_, file) => file)
                .toSeq
                .traverse { (imageMeta, imageFiles) =>
                  dbUtility.rollbackOnFailure { case given DBSession =>
                    val updatedMeta = imageMeta.copy(images = imageFiles)
                    imageRepository.update(updatedMeta, updatedMeta.id.get).map(_ => ())
                  }
                }
            }
        }

        Await.result(storeResultsFuture, 5.minutes)
      }
      .collectFirst { case Failure(ex) =>
        ex
      } match {
      case Some(ex) => Failure(ex)
      case None     => Success(())
    }
  }

  // TODO: Remove this after completing variants migration of existing images
  private def generateAndUploadVariantsForImageFileDataAsync(
      ignoreMissingObjects: Boolean
  )(imageMeta: ImageMetaInformation, imageFile: ImageFileData)(using
      ExecutionContext
  ): Future[Try[(ImageMetaInformation, ImageFileData)]] = Future {
    val imageStream = for {
      s3Object <- imageStorage.getRaw(imageFile.fileName)
      stream   <- imageConverter.s3ObjectToImageStream(s3Object)
    } yield stream

    imageStream match {
      case Success(ProcessableImageStream(image = img, format = fmt)) =>
        val dimensions = ImageDimensions(img.width, img.height)
        val fileStem   = imageFile.getFileStem
        Success((img, dimensions, fileStem, fmt))
      // We only process image/jpeg and image/png in this job, so an unprocessable format is an error
      case Success(UnprocessableImageStream(contentType = contentType)) =>
        Failure(ImageUnprocessableFormatException(contentType))
      case Failure(ex) => Failure(ex)
    }
  }.flatMap {
    case Success((img, dimensions, fileStem, format)) =>
      generateAndUploadVariantsAsync(img, dimensions, fileStem, format).map {
        case Success(variants) => Success(imageMeta -> imageFile.copy(variants = variants))
        case Failure(ex)       =>
          logger.error(
            s"Failed to generate/upload variants for image (imageMetaId = ${imageMeta.id.get}, fileName = ${imageFile.fileName})",
            ex,
          )
          Failure(ex)
      }
    case Failure(ex: MissingBucketKeyException) if ignoreMissingObjects =>
      logger.warn(
        s"Ignoring missing bucket object for image (imageMetaId = ${imageMeta.id.get}, fileName = ${imageFile.fileName})"
      )
      Future.successful(Success(imageMeta -> imageFile))
    case Failure(ex: ImageUnprocessableFormatException) =>
      logger.warn(
        s"Found image with JPEG/PNG Content-Type with invalid format (imageMetaId = ${imageMeta.id.get}, fileName = ${imageFile.fileName})",
        ex,
      )
      Future.successful(Success(imageMeta -> imageFile))
    case Failure(ex) => Future.successful(Failure(ex))
  }

  private[service] def getFileExtension(fileName: String): Option[String] = {
    fileName.lastIndexOf(".") match {
      case index: Int if index > -1 => Some(fileName.substring(index))
      case _                        => None
    }
  }

  private[service] def uploadImageWithVariants(file: UploadedFile): Try[UploadedImage] = {
    val extension      = file.fileName.flatMap(getFileExtension).getOrElse("")
    val uniqueFileStem = LazyList
      .continually(random.string(12))
      .dropWhile(stem => imageStorage.objectExists(s"$stem$extension"))
      .head
    val fileName = s"$uniqueFileStem$extension"

    val processableStream = imageConverter.uploadedFileToImageStream(file, fileName) match {
      case Success(stream: ProcessableImageStream)                      => stream
      case Success(stream @ UnprocessableImageStream(dimensions = dim)) => return uploadImageStream(stream, dim)
      case Failure(ex)                                                  => return Failure(ex)
    }

    val image      = processableStream.image
    val dimensions = ImageDimensions(image.width, image.height)

    Using(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(ImageVariantSize.values.size))) {
      case given ExecutionContext =>
        val variantsFuture             = generateAndUploadVariantsAsync(image, dimensions, uniqueFileStem, processableStream.format)
        val maybeUploadedOriginalImage = uploadImageStream(processableStream, Some(dimensions))
        val maybeVariants              = Await.result(variantsFuture, 1.minute)

        (maybeUploadedOriginalImage, maybeVariants) match {
          case (Success(uploadedImage), Success(variants)) => Success(uploadedImage.copy(variants = variants))
          case (Failure(ex), variants)                     =>
            variants.foreach(v => imageStorage.deleteObjects(v.map(_.bucketKey)))
            Failure(ex)
          case (original, Failure(ex)) =>
            original.foreach(i => imageStorage.deleteObject(i.fileName))
            Failure(ex)
        }
    }.flatten
  }

  /** Generate and upload image variants for `image`. Returns an [[Either]] with a [[Left]] value if one of the uploads
    * failed (containing the errors encountered), otherwise a [[Right]] value with all uploaded variants.
    */
  private def generateAndUploadVariantsAsync(
      image: ImmutableImage,
      dimensions: ImageDimensions,
      fileStem: String,
      format: ProcessableImageFormat,
  )(using ExecutionContext): Future[Try[Seq[ImageVariant]]] = {
    val variantSizes = ImageVariantSize.forDimensions(dimensions)
    if (variantSizes.size <= 0) {
      return Future.successful(Success(Seq.empty))
    }

    variantSizes
      .traverse { variantSize =>
        Future {
          for {
            resizedImage <- imageConverter.resizeToVariantSize(image, variantSize)
            imageBytes   <- Try(resizedImage.bytes(getWriterForFormat(format)))
            stream        = new ByteArrayInputStream(imageBytes)
            bucketKey     = s"$fileStem/${variantSize.entryName}.webp"
            imageVariant <- imageStorage
              .uploadFromStream(bucketKey, stream, imageBytes.length, "image/webp")
              .map(_ => ImageVariant(variantSize, bucketKey))
          } yield imageVariant
        }
      }
      .map { results =>
        val (failures, successes) = results.partitionMap(_.toEither)
        failures match {
          case Seq()            => Success(successes)
          case uploadExceptions =>
            val deleteResult = imageStorage.deleteObjects(successes.map(_.bucketKey))
            val exs          = uploadExceptions ++ deleteResult.failed.toOption
            Failure(ImageVariantsUploadException("Failed to upload image variant(s)", exs))
        }
      }
  }

  private def uploadImageStream(imageStream: ImageStream, dimensions: Option[ImageDimensions]): Try[UploadedImage] = {
    val contentLength = imageStream.contentLength
    val contentType   = imageStream.contentType
    for {
      imageInputStream <- imageStream.toStream
      bucketKey        <- imageStorage.uploadFromStream(imageStream.fileName, imageInputStream, contentLength, contentType)
    } yield UploadedImage(bucketKey, contentLength, contentType, dimensions, Seq.empty)
  }

  private def deleteImageAndVariants(image: ImageFileData): Try[Unit] = {
    val variantsResult = imageStorage.deleteObjects(image.variants.map(_.bucketKey))
    val imageResult    = imageStorage.deleteObject(image.fileName)

    variantsResult.failed.toOption ++ imageResult.failed.toOption match {
      case Nil => Success(())
      case exs => Failure(ImageDeleteException("Failed to delete original image and/or variants", exs.toSeq))
    }
  }

  private def moveImageAndVariants(image: ImageFileData, newBucketPrefix: String): Try[ImageFileData] = {
    val variantKeysToNewVariants = image
      .variants
      .map(variant => variant.bucketKey -> variant.copy(bucketKey = s"$newBucketPrefix/${variant.sizeName}.webp"))
    val variantKeysToNewKeys = variantKeysToNewVariants.map(entry => entry.fmap(_.bucketKey))
    val fileNameKeyToNewKey  = image.fileName -> s"$newBucketPrefix${getFileExtension(image.fileName)}"

    imageStorage.moveObjects(variantKeysToNewKeys :+ fileNameKeyToNewKey) match {
      case Success(_) =>
        Success(image.copy(fileName = fileNameKeyToNewKey._2, variants = variantKeysToNewVariants.map(_._2)))
      case Failure(ex) => Failure(ex)
    }
  }
}

object WriteService {
  // See https://developers.google.com/speed/webp/docs/cwebp#options
  // For PNG images, we set the quality to the default value in cwebp
  // For JPEG and WebP images, we set the quality higher in order to reduce the effect of double compression
  private val baseWebpWriter                = WebpWriter.DEFAULT.withMultiThread().withM(6)
  private val webpWriterForJpeg: WebpWriter = baseWebpWriter.withoutAlpha().withQ(85)
  private val webpWriterForPng: WebpWriter  = baseWebpWriter.withQ(75)
  private val webpWriterForWebp: WebpWriter = baseWebpWriter.withQ(85)

  def getWriterForFormat(format: ProcessableImageFormat): WebpWriter = format match {
    case ProcessableImageFormat.Jpeg => webpWriterForJpeg
    case ProcessableImageFormat.Png  => webpWriterForPng
    case ProcessableImageFormat.Webp => webpWriterForWebp
  }
}
