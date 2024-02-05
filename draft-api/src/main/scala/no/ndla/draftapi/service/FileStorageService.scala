/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.UploadedFile
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.AmazonClient

import scala.util.{Failure, Success, Try}

trait FileStorageService {
  this: AmazonClient with Props =>
  val fileStorage: FileStorageService

  class FileStorageService extends StrictLogging {
    import props.AttachmentStorageName

    private val resourceDirectory = "resources"

    def uploadResourceFromStream(
        stream: UploadedFile,
        contentType: String,
        storageKey: String
    ): Try[ObjectMetadata] = {
      val uploadPath = s"$resourceDirectory/$storageKey"

      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(stream.fileSize)

      val request = new PutObjectRequest(AttachmentStorageName, uploadPath, stream.stream, metadata)
      Try(amazonClient.putObject(request)) match {
        case Success(_)         => getObjectMetaData(uploadPath)
        case Failure(exception) => Failure(exception)
      }

    }

    def getObjectMetaData(storageKey: String): Try[ObjectMetadata] =
      Try(amazonClient.getObjectMetadata(AttachmentStorageName, storageKey))

    def resourceExists(storageKey: String): Boolean = resourceWithPathExists(s"$resourceDirectory/$storageKey")

    def copyResource(existingStorageKey: String, newStorageKey: String): Try[String] = {
      val uploadPath = s"$resourceDirectory/$newStorageKey"

      Try(
        amazonClient.copyObject(
          AttachmentStorageName,
          existingStorageKey,
          AttachmentStorageName,
          uploadPath
        )
      )
        .map(_ => uploadPath)
    }

    def resourceWithPathExists(filePath: String): Boolean =
      Try(amazonClient.doesObjectExist(AttachmentStorageName, filePath)).getOrElse(false)

    def deleteResource(storageKey: String): Try[_] = deleteResourceWithPath(s"$resourceDirectory/$storageKey")

    def deleteResourceWithPath(filePath: String): Try[_] =
      Try(amazonClient.deleteObject(AttachmentStorageName, filePath))
  }

}
