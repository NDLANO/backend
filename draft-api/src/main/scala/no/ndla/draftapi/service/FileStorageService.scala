/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.aws.NdlaS3Client
import no.ndla.common.model.domain.UploadedFile
import no.ndla.draftapi.Props
import software.amazon.awssdk.services.s3.model.HeadObjectResponse

import scala.util.Try

trait FileStorageService {
  this: NdlaS3Client with Props =>
  val fileStorage: FileStorageService

  class FileStorageService extends StrictLogging {
    val resourceDirectory = "resources"

    def uploadResourceFromStream(stream: UploadedFile, storageKey: String): Try[HeadObjectResponse] = {
      val uploadPath = s"$resourceDirectory/$storageKey"

      for {
        _    <- s3Client.putObject(uploadPath, stream)
        head <- s3Client.headObject(uploadPath)
      } yield head
    }

    def resourceExists(storageKey: String): Boolean = resourceWithPathExists(s"$resourceDirectory/$storageKey")

    def copyResource(existingStorageKey: String, newStorageKey: String): Try[String] = {
      val uploadPath = s"$resourceDirectory/$newStorageKey"
      s3Client.copyObject(existingStorageKey, uploadPath).map(_ => uploadPath)
    }

    def resourceWithPathExists(filePath: String): Boolean =
      s3Client.objectExists(filePath)

    def deleteResource(storageKey: String): Try[_] =
      deleteResourceWithPath(s"$resourceDirectory/$storageKey")

    def deleteResourceWithPath(filePath: String): Try[_] =
      s3Client.deleteObject(filePath)
  }

}
