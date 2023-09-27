/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import no.ndla.audioapi.Props
import no.ndla.audioapi.integration.AmazonClient
import no.ndla.common.model.domain.UploadedFile

import scala.util.{Failure, Success, Try}

trait AudioStorageService {
  this: AmazonClient with Props =>
  val audioStorage: AudioStorage

  class AudioStorage {
    import props.StorageName

    def storeAudio(
        audioStream: UploadedFile,
        contentType: String,
        destinationPath: String
    ): Try[ObjectMetadata] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(audioStream.fileSize)

      val request = new PutObjectRequest(StorageName, destinationPath, audioStream.stream, metadata)
      Try(amazonClient.putObject(request)) match {
        case Success(_)         => getObjectMetaData(destinationPath)
        case Failure(exception) => Failure(exception)
      }
    }

    def getObjectMetaData(storageKey: String): Try[ObjectMetadata] =
      Try(amazonClient.getObjectMetadata(StorageName, storageKey))

    def objectExists(storageKey: String): Boolean =
      getObjectMetaData(storageKey).isSuccess

    def deleteObject(storageKey: String): Try[Unit] = Try(amazonClient.deleteObject(StorageName, storageKey))

  }
}
