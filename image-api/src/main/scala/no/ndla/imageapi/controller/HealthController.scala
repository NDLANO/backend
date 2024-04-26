/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.{Eff, Props}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.network.tapir.TapirHealthController

trait HealthController {
  this: ImageStorageService & ImageRepository & Props & TapirHealthController =>
  val healthController: HealthController

  class HealthController extends TapirHealthController[Eff] {

    override def checkHealth(): Either[String, String] = {
      imageRepository
        .getRandomImage()
        .map(image => {
          image.images
            .map(imgMeta => {
              imgMeta.headOption
                .map(img => {
                  if (imageStorage.objectExists(img.fileName)) {
                    Right("Healthy")
                  } else {
                    Left("Internal server error")
                  }
                })
                .getOrElse(Right("Healthy"))
            })
            .getOrElse(Right("Healthy"))
        })
        .getOrElse(Right("Healthy"))
    }
  }

}
