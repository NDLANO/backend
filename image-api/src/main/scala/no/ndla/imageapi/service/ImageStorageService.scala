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
import no.ndla.imageapi.model.domain.{ImageContentType, ImageStream}

import java.io.InputStream
import scala.util.{Failure, Success, Try}

class ImageStorageService(using
    s3Client: => NdlaS3Client,
    readService: ReadService,
    imageConverter: ImageConverter,
    props: Props,
) extends StrictLogging {
  private def attemptFixS3ContentType(s3Object: NdlaS3Object): Unit =
    if (s3Object.contentType == "binary/octet-stream") {
      val fileName = s3Object.key
      readService.getImageFileFromFilePath(fileName) match {
        case Success(meta) if meta.contentType.toString != "binary/octet-stream" =>
          if (props.ValidMimeTypes.contains(meta.contentType)) {
            s3Client.updateContentType(s3Object.key, meta.contentType.toString) match {
              case Success(_)  => logger.info(s"Updated Content-Type of $fileName to ${meta.contentType}")
              case Failure(ex) => logger.error(s"Failed to update Content-Type of $fileName to ${meta.contentType}", ex)
            }
          } else {
            logger.error(s"Content-Type of '$fileName' is '${meta.contentType}', which is not a supported MIME type")
          }
        case Success(_)  => ()
        case Failure(ex) => logger.error(s"Failed to get image from file path: $fileName", ex)
      }
    }

  def get(imageKey: String): Try[ImageStream] = {
    for {
      s3Object    <- s3Client.getObject(imageKey)
      imageStream <- imageConverter.s3ObjectToImageStream(s3Object)
      _            = attemptFixS3ContentType(s3Object)
    } yield imageStream
  }

  def getRaw(bucketKey: String): Try[NdlaS3Object] = s3Client.getObject(bucketKey)

  def getUrl(bucketKey: String): Try[String] = s3Client.getUrl(bucketKey)

  def uploadFromStream(
      storageKey: String,
      stream: InputStream,
      contentLength: Long,
      contentType: ImageContentType,
  ): Try[String] = s3Client
    .putObject(storageKey, stream, contentLength, contentType.toString, props.S3NewFileCacheControlHeader.some)
    .map(_ => storageKey)

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
