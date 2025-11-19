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
    val maybeHealthy = for {
      imageMeta <- imageRepository
        .getRandomImage()
        .recover { ex =>
          logger.error("Failed to fetch random image in health check", ex)
          None
        }
        .toOption
        .flatten
      imageFile <- imageMeta.images.headOption
      healthy   <- Option.when(imageStorageService.objectExists(imageFile.fileName))("Healthy")
    } yield healthy

    maybeHealthy.toRight("Internal server error")
  }
}
