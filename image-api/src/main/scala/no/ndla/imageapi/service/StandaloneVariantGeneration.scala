/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.MissingBucketKeyException
import no.ndla.database.DBUtility
import no.ndla.imageapi.model.ImageUnprocessableFormatException
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.repository.ImageRepository

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Properties.propOrNone
import scala.util.{Failure, Success, Try, Using}

class StandaloneVariantGeneration(
    imageConverter: ImageConverter,
    writeService: WriteService,
    imageStorage: ImageStorageService,
    imageRepository: ImageRepository,
    dbUtility: DBUtility,
) extends StrictLogging {
  private val ProcessableContentTypes: Set[ImageContentType] = Set(ImageContentType.Png, ImageContentType.Jpeg)
  private val BatchSize                                      = 20

  def doStandaloneVariantGeneration(): Nothing = {
    val mode = propOrNone("STANDALONE_VARIANT_GENERATION_MODE")
      .flatMap(ImageVariantGenerationMode.withNameOption)
      .getOrElse {
        throw IllegalArgumentException(
          s"Invalid or missing STANDALONE_VARIANT_GENERATION_MODE. Expected one of: " +
            ImageVariantGenerationMode.values.map(_.entryName).mkString(", ")
        )
      }

    logger.info(s"Starting standalone image variant generation in '${mode.entryName}' mode")

    generateVariantsForExistingImages(mode) match {
      case Success(_) =>
        logger.info("Standalone image variant generation finished successfully")
        sys.exit(0)
      case Failure(ex) =>
        logger.error("Standalone image variant generation failed", ex)
        sys.exit(1)
    }
  }

  def generateVariantsForExistingImages(mode: ImageVariantGenerationMode): Try[Unit] = {
    val batchIterator = imageRepository.getImageMetaBatched(BatchSize) match {
      case Success(iterator) => iterator
      case Failure(ex)       => return Failure(ex)
    }
    val totalBatchCount = batchIterator.knownSize

    Using(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(BatchSize))) {
      case given ExecutionContext => batchIterator
          .zipWithIndex
          .map { (batch, index) =>
            logger.info(s"Processing batch ${index + 1} of $totalBatchCount (batch size = $BatchSize)")

            val batchFuture = Future.traverse(batch) { imageMeta =>
              processImageMeta(imageMeta, mode)
            }

            Try(Await.result(batchFuture, 5.minutes))
          }
          .collectFirst { case Failure(ex) =>
            ex
          } match {
          case Some(ex) => Failure(ex)
          case None     => Success(())
        }
    }.flatten
  }

  private def processImageMeta(imageMeta: ImageMetaInformation, mode: ImageVariantGenerationMode)(using
      ExecutionContext
  ): Future[Unit] = {
    val imageMetaId = imageMeta.id match {
      case Some(id) => id
      case None     =>
        logger.error("Found image meta without ID, which should not happen. Skipping this image meta.", new Throwable)
        return Future.successful(())
    }
    val filesToProcess = imageMeta.images.filter(shouldProcess(_, mode))

    Future
      .traverse(filesToProcess) { imageFile =>
        generateAndUploadVariantsForImageFileDataAsync(imageMetaId, imageFile)
      }
      .flatMap { generatedFiles =>
        val updatedMeta = imageMeta.copy(images =
          imageMeta
            .images
            .map { imageFile =>
              generatedFiles
                .find(updated => updated.fileName == imageFile.fileName && updated.language == imageFile.language)
                .getOrElse(imageFile)
            }
        )

        dbUtility
          .rollbackOnFailure(implicit session => imageRepository.update(updatedMeta, imageMetaId))
          .map(_ => deleteObsoleteVariantKeys(imageMeta, updatedMeta, mode))
          .liftTo[Future]
      }
  }

  private def shouldProcess(imageFile: ImageFileData, mode: ImageVariantGenerationMode): Boolean = {
    val isProcessableType = ProcessableContentTypes.contains(imageFile.contentType)
    mode match {
      case _ if !isProcessableType                => false
      case ImageVariantGenerationMode.MissingOnly => imageFile.dimensions.map(ImageVariantSize.forDimensions) match {
          case Some(expectedSizes) =>
            val actualSizes = imageFile.variants.map(_.size)
            actualSizes != expectedSizes
          case None => true
        }
      case ImageVariantGenerationMode.ReplaceAll => true
    }
  }

  private[service] def generateAndUploadVariantsForImageFileDataAsync(imageMetaId: Long, imageFile: ImageFileData)(using
      ExecutionContext
  ): Future[ImageFileData] = Future {
    for {
      s3Object          <- imageStorage.getRaw(imageFile.fileName)
      processableStream <- imageConverter
        .s3ObjectToImageStream(s3Object)
        .flatMap {
          case stream: ImageStream.Processable => Success(stream)
          // We only process image/jpeg and image/png in this job, so a GIF or unprocessable image is an error
          case stream: (
                ImageStream.Gif | ImageStream.Unprocessable
              ) => Failure(ImageUnprocessableFormatException(stream.contentType.toString))
        }
        .recoverWith { ex =>
          Try(s3Object.stream.close()) match {
            case Success(_)       => Failure(ex)
            case Failure(closeEx) =>
              ex.addSuppressed(closeEx)
              Failure(ex)
          }
        }
      processableImage <- ProcessableImage.fromStream(processableStream)
      dimensions        = ImageDimensions(processableImage.image.width, processableImage.image.height)
      fileStem          = imageFile.getFileStem
    } yield (processableImage, dimensions, fileStem, processableImage.format)
  }.flatMap {
    case Success((img, dimensions, fileStem, format)) => writeService
        .generateAndUploadVariantsAsync(img, dimensions, fileStem, format)
        .transform(
          variants => imageFile.copy(variants = variants, dimensions = Some(dimensions)),
          { ex =>
            logger.error(
              s"Failed to generate/upload variants for image (imageMetaId = $imageMetaId, fileName = ${imageFile.fileName})",
              ex,
            )
            ex
          },
        )
    case Failure(ex: MissingBucketKeyException) =>
      logger.warn(
        s"Ignoring missing bucket object for image (imageMetaId = $imageMetaId, fileName = ${imageFile.fileName})"
      )
      Future.successful(imageFile)
    case Failure(ex: ImageUnprocessableFormatException) =>
      logger.warn(
        s"Found image with JPEG/PNG Content-Type with invalid format (imageMetaId = $imageMetaId, fileName = ${imageFile.fileName})",
        ex,
      )
      Future.successful(imageFile)
    case Failure(ex) => Future.failed(ex)
  }

  private def deleteObsoleteVariantKeys(
      originalMeta: ImageMetaInformation,
      updatedMeta: ImageMetaInformation,
      mode: ImageVariantGenerationMode,
  ): Unit = mode match {
    case ImageVariantGenerationMode.MissingOnly => ()
    case ImageVariantGenerationMode.ReplaceAll  =>
      val obsoleteKeySets = for {
        original    <- originalMeta.images
        originalKeys = original.variants.map(_.bucketKey).toSet
        updated     <- updatedMeta.images.find(u => u.fileName == original.fileName && u.language == original.language)
        updatedKeys  = updated.variants.map(_.bucketKey).toSet
        obsoleteKeys = originalKeys.diff(updatedKeys)
      } yield obsoleteKeys

      val obsoleteKeys = obsoleteKeySets.foldLeft(Set.empty[String])((res, elem) => res.union(elem))
      if (obsoleteKeys.nonEmpty) {
        imageStorage.deleteObjects(obsoleteKeys.toSeq) match {
          case Success(_)  => ()
          case Failure(ex) => logger.error(
              s"Failed to delete obsolete variant keys for imageMetaId = ${originalMeta.id.get}. Obsolete keys: ${obsoleteKeys.mkString("'", "', '", "'")}",
              ex,
            )
        }
      }
  }
}
