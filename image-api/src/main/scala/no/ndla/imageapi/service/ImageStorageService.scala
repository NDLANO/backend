/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, InputStream}
import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.StrictLogging

import javax.imageio.ImageIO
import no.ndla.imageapi.Props
import no.ndla.imageapi.integration.AmazonClient
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.model.domain.ImageStream
import org.apache.commons.io.IOUtils

import scala.util.{Failure, Success, Try}

trait ImageStorageService {
  this: AmazonClient with ReadService with Props =>
  val imageStorage: AmazonImageStorageService

  class AmazonImageStorageService extends StrictLogging {
    import props.{StorageName, ValidMimeTypes}
    case class NdlaImage(s3Object: S3Object, fileName: String) extends ImageStream {
      lazy val imageContent: Array[Byte] = {
        val content = IOUtils.toByteArray(s3Object.getObjectContent)
        s3Object.getObjectContent.close()
        content
      }

      override val sourceImage: BufferedImage = ImageIO.read(stream)

      override def contentType: String = {
        val s3ContentType = s3Object.getObjectMetadata.getContentType
        if (s3ContentType == "binary/octet-stream") {
          readService.getImageFromFilePath(fileName) match {
            case Failure(ex) =>
              logger.warn(s"Couldn't get meta for $fileName so using s3 content-type of '$s3ContentType'", ex)
              s3ContentType
            case Success(meta)
                if meta.contentType != "" && meta.contentType != "binary/octet-stream" && ValidMimeTypes.contains(
                  meta.contentType
                ) =>
              updateContentType(s3Object.getKey, meta.contentType) match {
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
      Try(amazonClient.getObject(new GetObjectRequest(StorageName, imageKey))).map(s3Object =>
        NdlaImage(s3Object, imageKey)
      ) match {
        case Success(e) => Success(e)
        case Failure(ex: AmazonS3Exception) if ex.getStatusCode == 404 =>
          Failure(new ImageNotFoundException(s"Image $imageKey does not exist"))
        case Failure(ex) =>
          logger.error(s"Failed to get image '$imageKey' from S3", ex)
          Failure(ex)
      }
    }

    def uploadFromStream(stream: InputStream, storageKey: String, contentType: String, size: Long): Try[String] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size)
      metadata.setCacheControl(props.S3NewFileCacheControlHeader)

      Try(amazonClient.putObject(new PutObjectRequest(StorageName, storageKey, stream, metadata))).map(_ => storageKey)
    }

    def updateContentType(storageKey: String, contentType: String): Try[Unit] = {
      Try {
        val meta = amazonClient.getObjectMetadata(StorageName, storageKey)
        meta.setContentType(contentType)

        val copyRequest = new CopyObjectRequest(StorageName, storageKey, StorageName, storageKey)
          .withNewObjectMetadata(meta)

        amazonClient.copyObject(copyRequest): Unit
      }
    }

    def cloneObject(existingKey: String, newKey: String): Try[Unit] = {
      Try(amazonClient.copyObject(StorageName, existingKey, StorageName, newKey): Unit)
    }

    def objectExists(storageKey: String): Boolean = {
      Try(amazonClient.doesObjectExist(StorageName, storageKey)).getOrElse(false)
    }

    def objectSize(storageKey: String): Long = {
      Try(amazonClient.getObjectMetadata(StorageName, storageKey)).map(_.getContentLength).getOrElse(0)
    }

    def deleteObject(storageKey: String): Try[Unit] = Try(amazonClient.deleteObject(StorageName, storageKey))
  }

}
