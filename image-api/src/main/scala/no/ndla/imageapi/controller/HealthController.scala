/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.network.tapir.TapirHealthController

trait HealthController {
  this: ImageStorageService & ImageRepository & Props & TapirHealthController =>
  lazy val healthController: HealthController

  class HealthController extends TapirHealthController {

    override def checkReadiness(): Either[String, String] = {
      imageRepository
        .getRandomImage()
        .flatMap(image => {
          image.images
            .flatMap(imgMeta => {
              imgMeta.headOption
                .map(img => {
                  if (imageStorage.objectExists(img.fileName)) {
                    Right("Healthy")
                  } else {
                    Left("Internal server error")
                  }
                })
            })
        })
        .getOrElse(Right("Healthy"))
    }
  }

}
