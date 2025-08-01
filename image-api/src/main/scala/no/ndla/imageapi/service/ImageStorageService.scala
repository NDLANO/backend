/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits.catsSyntaxOptionId

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, InputStream}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.aws.{NdlaS3Client, NdlaS3Object}
import no.ndla.common.model.domain.UploadedFile

import javax.imageio.ImageIO
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.model.domain.ImageStream
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

import scala.util.{Failure, Success, Try}

trait ImageStorageService {
  this: NdlaS3Client & ReadService & Props =>
  val imageStorage: AmazonImageStorageService

  class AmazonImageStorageService extends StrictLogging {
    import props.ValidMimeTypes
    case class NdlaImage(s3Object: NdlaS3Object, fileName: String) extends ImageStream {
      lazy val imageContent: Array[Byte]      = s3Object.stream.readAllBytes()
      override val sourceImage: BufferedImage = ImageIO.read(stream)

      override def contentType: String = {
        val s3ContentType = s3Object.contentType
        if (s3ContentType == "binary/octet-stream") {
          readService.getImageFromFilePath(fileName) match {
            case Failure(ex) =>
              logger.warn(s"Couldn't get meta for $fileName so using s3 content-type of '$s3ContentType'", ex)
              s3ContentType
            case Success(meta)
                if meta.contentType != "" && meta.contentType != "binary/octet-stream" && ValidMimeTypes.contains(
                  meta.contentType
                ) =>
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

      override def stream: InputStream = new ByteArrayInputStream(imageContent)
    }

    def get(imageKey: String): Try[ImageStream] = {
      s3Client.getObject(imageKey).map(s3Object => NdlaImage(s3Object, imageKey)) match {
        case Success(e)                     => Success(e)
        case Failure(_: NoSuchKeyException) =>
          Failure(new ImageNotFoundException(s"Image $imageKey does not exist"))
        case Failure(ex) =>
          logger.error(s"Failed to get image '$imageKey' from S3", ex)
          Failure(ex)
      }
    }

    def uploadFromStream(storageKey: String, uploadedFile: UploadedFile): Try[String] = {
      s3Client.putObject(storageKey, uploadedFile, props.S3NewFileCacheControlHeader.some).map(_ => storageKey)
    }

    def updateContentType(storageKey: String, contentType: String): Try[Unit] =
      for {
        meta <- s3Client.headObject(storageKey)
        metadata = meta.metadata()
        _        = metadata.put("Content-Type", contentType)
        _ <- s3Client.updateMetadata(storageKey, metadata)
      } yield ()

    def cloneObject(existingKey: String, newKey: String): Try[Unit] = {
      s3Client.copyObject(existingKey, newKey).map(_ => ())
    }

    def objectExists(storageKey: String): Boolean =
      s3Client.objectExists(storageKey)

    def deleteObject(storageKey: String): Try[Unit] = s3Client.deleteObject(storageKey).map(_ => ())
  }

}
