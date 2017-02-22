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
    def storeAudio(audioUrl: URL, contentType: String, size: String, destinationPath: String): Try[String] = {
      storeAudio(audioUrl.openStream, contentType, size.toLong, destinationPath)
    }

    def storeAudio(audioStream: InputStream, contentType: String, size: Long, destinationPath: String): Try[String] = {
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

    def deleteObject(storageKey: String): Try[_] = {
      Try(amazonClient.deleteObject(StorageName, storageKey))
    }


  }
}
