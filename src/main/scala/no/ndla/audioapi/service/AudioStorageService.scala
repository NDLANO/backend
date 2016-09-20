/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import java.net.URL
import scala.util.{Failure, Try}

trait AudioStorageService {
  val audioStorage: AudioStorage

  implicit def stringToUrl(url: String): URL = new URL(url)

  class AudioStorage {

    def storeAudio(uRL: URL, destinationPath: String): Try[String] = {
      Failure(new Exception)
    }

  }
}
