/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.{Eff, Props}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.common.aws.NdlaS3Client
import no.ndla.network.tapir.TapirHealthController

trait HealthController {
  this: NdlaS3Client & AudioRepository & Props & TapirHealthController =>
  val healthController: HealthController

  class HealthController extends TapirHealthController[Eff] {

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

}
