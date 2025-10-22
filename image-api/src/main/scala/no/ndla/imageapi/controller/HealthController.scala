/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirHealthController}

class HealthController(using
    imageStorageService: ImageStorageService,
    imageRepository: ImageRepository,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirHealthController {

  override def checkReadiness(): Either[String, String] = {
    imageRepository
      .getRandomImage()
      .flatMap(image => {
        image
          .images
          .flatMap(imgMeta => {
            imgMeta
              .headOption
              .map(img => {
                if (imageStorageService.objectExists(img.fileName)) {
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
