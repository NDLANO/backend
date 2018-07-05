/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import java.io.InputStream
import java.net.URL

import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import no.ndla.audioapi.AudioApiProperties.StorageName
import no.ndla.audioapi.integration.AmazonClient

import scala.util.{Failure, Success, Try}

trait AudioStorageService {
  this: AmazonClient =>
  val audioStorage: AudioStorage

  class AudioStorage {

    def storeAudio(audioUrl: URL, contentType: String, size: String, destinationPath: String): Try[ObjectMetadata] = {
      storeAudio(audioUrl.openStream, contentType, size.toLong, destinationPath)
    }

    def storeAudio(audioStream: InputStream,
                   contentType: String,
                   size: Long,
                   destinationPath: String): Try[ObjectMetadata] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)

      val request = new PutObjectRequest(StorageName, destinationPath, audioStream, metadata)
      Try(amazonClient.putObject(request)) match {
        case Success(_)         => getObjectMetaData(destinationPath)
        case Failure(exception) => Failure(exception)
      }
    }

    def getObjectMetaData(storageKey: String): Try[ObjectMetadata] =
      Try(amazonClient.getObjectMetadata(StorageName, storageKey))

    def objectExists(storageKey: String): Boolean =
      getObjectMetaData(storageKey).isSuccess

    def deleteObject(storageKey: String): Try[_] = {
      Try(amazonClient.deleteObject(StorageName, storageKey))
    }

  }
}
