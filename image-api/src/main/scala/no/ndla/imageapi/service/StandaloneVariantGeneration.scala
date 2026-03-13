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
import no.ndla.common.errors.MissingIdException
import no.ndla.common.implicits.*
import no.ndla.database.DBUtility
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.domain.{ImageContentType, ImageFileData, ImageMetaInformation, ImageVariantGenerationMode}
import no.ndla.imageapi.repository.ImageRepository
import scalikejdbc.DBSession

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try, Using}

class StandaloneVariantGeneration(
    writeService: WriteService,
    imageStorage: ImageStorageService,
    imageRepository: ImageRepository,
    dbUtility: DBUtility,
    props: ImageApiProperties,
) extends StrictLogging {
  private val ProcessableContentTypes: Set[ImageContentType] = Set(ImageContentType.Png, ImageContentType.Jpeg)
  private val BatchSize                                      = 20

  def doStandaloneVariantGeneration(): Nothing = {
    val mode = props
      .StandaloneVariantGenerationMode
      .flatMap(ImageVariantGenerationMode.withNameOption)
      .getOrElse {
        throw IllegalArgumentException(
          s"Invalid or missing STANDALONE_VARIANT_GENERATION_MODE. Expected one of: " +
            ImageVariantGenerationMode.values.map(_.entryName).mkString(", ")
        )
      }

    logger.info(s"Starting standalone image variant generation in '${mode.entryName}' mode")

    generateVariantsForExistingImages(mode, props.StandaloneVariantGenerationIgnoreMissing) match {
      case Success(_) =>
        logger.info("Standalone image variant generation finished successfully")
        sys.exit(0)
      case Failure(ex) =>
        logger.error("Standalone image variant generation failed", ex)
        sys.exit(1)
    }
  }

  def generateVariantsForExistingImages(mode: ImageVariantGenerationMode, ignoreMissingObjects: Boolean): Try[Unit] = {
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
              processImageMeta(imageMeta, mode, ignoreMissingObjects)
            }

            Await.result(batchFuture, 5.minutes).sequence
          }
          .collectFirst { case Failure(ex) =>
            ex
          } match {
          case Some(ex) => Failure(ex)
          case None     => Success(())
        }
    }.flatten
  }

  private def processImageMeta(
      imageMeta: ImageMetaInformation,
      mode: ImageVariantGenerationMode,
      ignoreMissingObjects: Boolean,
  )(using ExecutionContext): Future[Try[Unit]] = {
    val filesToProcess = imageMeta.images.filter(shouldProcess(_, mode))

    if (filesToProcess.isEmpty) {
      Future.successful(Success(()))
    } else {
      Future
        .traverse(filesToProcess) { imageFile =>
          writeService
            .generateAndUploadVariantsForImageFileDataAsync(ignoreMissingObjects)(imageMeta, imageFile)
            .map(_.map(_._2))
        }
        .map { generatedFilesT =>
          for {
            generatedFiles <- generatedFilesT.sequence
            imageId        <- imageMeta
              .id
              .toTry(MissingIdException("Could not update image variants for metadata without id. This is a bug."))
            updatedMeta = imageMeta.copy(images =
              imageMeta
                .images
                .map { imageFile =>
                  generatedFiles
                    .find(updated => updated.language == imageFile.language && updated.fileName == imageFile.fileName)
                    .getOrElse(imageFile)
                }
            )
            obsoleteKeys = obsoleteVariantKeys(imageMeta, updatedMeta, mode)
            _           <- dbUtility.rollbackOnFailure { case given DBSession =>
              imageRepository.update(updatedMeta, imageId).map(_ => ())
            }
            _ <- deleteObsoleteVariants(obsoleteKeys)
          } yield ()
        }
    }
  }

  private def shouldProcess(imageFile: ImageFileData, mode: ImageVariantGenerationMode): Boolean =
    ProcessableContentTypes.contains(imageFile.contentType) && (mode match {
      case ImageVariantGenerationMode.MissingOnly => imageFile.variants.isEmpty
      case ImageVariantGenerationMode.ReplaceAll  => true
    })

  private def obsoleteVariantKeys(
      originalMeta: ImageMetaInformation,
      updatedMeta: ImageMetaInformation,
      mode: ImageVariantGenerationMode,
  ): Seq[String] = mode match {
    case ImageVariantGenerationMode.MissingOnly => Seq.empty
    case ImageVariantGenerationMode.ReplaceAll  => originalMeta
        .images
        .zip(updatedMeta.images)
        .flatMap { (original, updated) =>
          val newKeys = updated.variants.map(_.bucketKey).toSet
          original.variants.map(_.bucketKey).filterNot(newKeys.contains)
        }
        .distinct
  }

  private def deleteObsoleteVariants(bucketKeys: Seq[String]): Try[Unit] =
    if (bucketKeys.isEmpty) Success(())
    else imageStorage.deleteObjects(bucketKeys)
}
