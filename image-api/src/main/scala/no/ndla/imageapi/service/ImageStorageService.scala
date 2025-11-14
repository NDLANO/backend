/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.aws.{NdlaS3Client, NdlaS3Object}
import no.ndla.common.model.domain.UploadedFile
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.ImageStream

import java.io.InputStream
import scala.util.{Failure, Success, Try}

class ImageStorageService(using
    s3Client: => NdlaS3Client,
    readService: ReadService,
    imageConverter: ImageConverter,
    props: Props,
) extends StrictLogging {
  private def ensureS3ContentType(s3Object: NdlaS3Object): String = {
    val s3ContentType = s3Object.contentType
    val fileName      = s3Object.key
    if (s3ContentType == "binary/octet-stream") {
      readService.getImageFromFilePath(fileName) match {
        case Failure(ex) =>
          logger.warn(s"Couldn't get meta for $fileName so using s3 content-type of '$s3ContentType'", ex)
          s3ContentType
        case Success(meta)
            if meta.contentType != "" && meta.contentType != "binary/octet-stream" && props
              .ValidMimeTypes
              .contains(meta.contentType) =>
          updateContentType(s3Object.key, meta.contentType) match {
            case Failure(ex) =>
              logger.error(s"Could not update content-type s3-metadata of $fileName to ${meta.contentType}", ex)
            case Success(_) =>
              logger.info(s"Successfully updated content-type s3-metadata of $fileName to ${meta.contentType}")
          }
          meta.contentType
        case _ => s3ContentType
      }
    } else s3ContentType
  }

  def get(imageKey: String): Try[ImageStream] = {
    for {
      s3Object    <- s3Client.getObject(imageKey)
      imageStream <- imageConverter.s3ObjectToImageStream(s3Object)
      _            = ensureS3ContentType(s3Object)
    } yield imageStream
  }

  def getRaw(bucketKey: String): Try[NdlaS3Object] = s3Client.getObject(bucketKey)

  def uploadFromStream(storageKey: String, uploadedFile: UploadedFile): Try[String] = {
    s3Client.putObject(storageKey, uploadedFile, props.S3NewFileCacheControlHeader.some).map(_ => storageKey)
  }

  def uploadFromStream(storageKey: String, stream: InputStream, contentLength: Long, contentType: String): Try[String] =
    s3Client
      .putObject(storageKey, stream, contentLength, contentType, props.S3NewFileCacheControlHeader.some)
      .map(_ => storageKey)

  def updateContentType(storageKey: String, contentType: String): Try[Unit] = for {
    meta    <- s3Client.headObject(storageKey)
    metadata = meta.metadata()
    _        = metadata.put("Content-Type", contentType)
    _       <- s3Client.updateMetadata(storageKey, metadata)
  } yield ()

  def objectExists(storageKey: String): Boolean = s3Client.objectExists(storageKey)

  def deleteObject(storageKey: String): Try[Unit] = s3Client.deleteObject(storageKey).map(_ => ())

  def deleteObjects(storageKeys: Seq[String]): Try[Unit] = s3Client.deleteObjects(storageKeys).map(_ => ())

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
