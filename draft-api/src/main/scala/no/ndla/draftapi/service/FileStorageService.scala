/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import java.io.InputStream

import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties.AttachmentStorageName
import no.ndla.draftapi.integration.AmazonClient

import scala.util.Try

trait FileStorageService {
  this: AmazonClient =>
  val fileStorage: FileStorageService

  class FileStorageService extends LazyLogging {
    private val resourceDirectory = "resources"

    def uploadResourceFromStream(stream: InputStream,
                                 storageKey: String,
                                 contentType: String,
                                 size: Long): Try[String] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size)

      val uploadPath = s"$resourceDirectory/$storageKey"

      Try(
        amazonClient
          .putObject(new PutObjectRequest(AttachmentStorageName, uploadPath, stream, metadata))
      ).map(_ => uploadPath)
    }

    def resourceExists(storageKey: String): Boolean = resourceWithPathExists(s"$resourceDirectory/$storageKey")

    def copyResource(existingStorageKey: String, newStorageKey: String): Try[String] = {
      val uploadPath = s"$resourceDirectory/$newStorageKey"

      Try(
        amazonClient.copyObject(
          AttachmentStorageName,
          existingStorageKey,
          AttachmentStorageName,
          uploadPath
        ))
        .map(_ => uploadPath)
    }

    def resourceWithPathExists(filePath: String): Boolean =
      Try(amazonClient.doesObjectExist(AttachmentStorageName, filePath)).getOrElse(false)

    def deleteResource(storageKey: String): Try[_] = deleteResourceWithPath(s"$resourceDirectory/$storageKey")

    def deleteResourceWithPath(filePath: String): Try[_] =
      Try(amazonClient.deleteObject(AttachmentStorageName, filePath))
  }

}
