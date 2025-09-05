/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.integration.NDLAS3Client
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.network.tapir.{ErrorHelpers, TapirHealthController}
import no.ndla.network.clients.MyNDLAApiClient

class HealthController(using
    s3Client: => NDLAS3Client,
    audioRepository: AudioRepository,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ControllerErrorHandling
) extends TapirHealthController {

  override def checkReadiness(): Either[String, String] = {
    audioRepository
      .getRandomAudio()
      .flatMap(audio => {
        audio.filePaths.headOption.map(filePath => {
          if (s3Client.objectExists(filePath.filePath)) {
            Right("Healthy")
          } else {
            Left("Internal server error")
          }
        })
      })
      .getOrElse(Right("Healthy"))
  }
}
