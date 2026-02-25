/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.data.NonEmptySeq
import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.aws.{NdlaS3Client, NdlaS3Object}
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ImageContentTypeException
import no.ndla.imageapi.model.domain.{ImageContentType, ImageStream}

import java.io.InputStream
import scala.util.{Failure, Success, Try}

class ImageStorageService(using
    s3Client: => NdlaS3Client,
    readService: ReadService,
    imageConverter: ImageConverter,
    props: Props,
) extends StrictLogging {
  private def ensureS3ContentType(s3Object: NdlaS3Object): Try[Unit] = {
    val fileName = s3Object.key
    if (s3Object.contentType == "binary/octet-stream") {
      readService.getImageFileFromFilePath(fileName) match {
        case Success(meta) if props.ValidMimeTypes.contains(meta.contentType) =>
          updateContentType(s3Object.key, meta.contentType) match {
            case Failure(ex) =>
              logger.error(s"Could not update content-type s3-metadata of $fileName to ${meta.contentType}", ex)
            case Success(_) =>
              logger.info(s"Successfully updated content-type s3-metadata of $fileName to ${meta.contentType}")
          }
          Success(())
        case _ => Failure(ImageContentTypeException("Image content type unknown"))
      }
    } else Success(())
  }

  def get(imageKey: String): Try[ImageStream] = {
    for {
      s3Object    <- s3Client.getObject(imageKey)
      imageStream <- imageConverter.s3ObjectToImageStream(s3Object)
      _            = ensureS3ContentType(s3Object)
    } yield imageStream
  }

  def getRaw(bucketKey: String): Try[NdlaS3Object] = s3Client.getObject(bucketKey)

  def uploadFromStream(
      storageKey: String,
      stream: InputStream,
      contentLength: Long,
      contentType: ImageContentType,
  ): Try[String] = s3Client
    .putObject(storageKey, stream, contentLength, contentType.toString, props.S3NewFileCacheControlHeader.some)
    .map(_ => storageKey)

  def updateContentType(storageKey: String, contentType: ImageContentType): Try[Unit] = for {
    meta    <- s3Client.headObject(storageKey)
    metadata = meta.metadata()
    _        = metadata.put("Content-Type", contentType.toString)
    _       <- s3Client.updateMetadata(storageKey, metadata)
  } yield ()

  def checkBucketAccess(): Try[Unit] = s3Client.canAccessBucket

  def objectExists(storageKey: String): Boolean = s3Client.objectExists(storageKey)

  def deleteObject(storageKey: String): Try[Unit] = s3Client.deleteObject(storageKey).map(_ => ())

  def deleteObjects(storageKeys: Seq[String]): Try[Unit] = storageKeys match {
    case head :: tail => s3Client.deleteObjects(NonEmptySeq(head, tail)).map(_ => ())
    case Nil          => Success(())
  }

  def moveObjects(fromKeysToKeys: Seq[(String, String)]): Try[Unit] = {
    fromKeysToKeys.traverse((fromKey, toKey) => s3Client.copyObject(fromKey, toKey)) match {
      case Success(_) =>
        deleteObjects(fromKeysToKeys.map(_._1)).handleError(ex =>
          logger.error("Failed to clean up old S3 objects when moving", ex)
        ): Unit
        Success(())
      case Failure(ex) => Failure(ex)
    }
  }
}
