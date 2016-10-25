/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import java.net.URL

import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import no.ndla.audioapi.integration.AmazonClient
import no.ndla.audioapi.AudioApiProperties.StorageName

import scala.util.{Failure, Success, Try}

trait AudioStorageService {
  this: AmazonClient =>
  val audioStorage: AudioStorage

  class AudioStorage {
    def storeAudio(audioUrl: URL, contentType: String, size: String, destinationPath: String): Try[String] = {
      if (objectExists(destinationPath)) {
        return Success(destinationPath)
      }

      val audioStream = audioUrl.openStream()
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size.toLong)

      val request = new PutObjectRequest(StorageName, destinationPath, audioStream, metadata)
      Try(amazonClient.putObject(request)) match {
        case Success(_) => Success(destinationPath)
        case Failure(exception) => Failure(exception)
      }
    }

    def objectExists(storageKey: String): Boolean = {
      Try(amazonClient.getObject(new GetObjectRequest(StorageName, storageKey))) match {
        case Success(obj) => {
          obj.close()
          true
        }
        case Failure(_) => false
      }
    }

  }
}
