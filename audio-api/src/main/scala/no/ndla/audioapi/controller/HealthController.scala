/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.common.aws.NdlaS3Client
import no.ndla.network.tapir.TapirHealthController

class HealthController(using
  s3Client: NdlaS3Client,
  audioRepository: AudioRepository,
  props: Props
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
