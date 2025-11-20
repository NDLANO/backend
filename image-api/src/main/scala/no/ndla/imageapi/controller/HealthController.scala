/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirHealthController}

import scala.util.{Failure, Success}

class HealthController(using
    imageStorageService: ImageStorageService,
    imageRepository: ImageRepository,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirHealthController {
  override def checkReadiness(): Either[String, String] = {
    val maybeHealth = for {
      imageMeta <- randomImage
      imageFile <- imageMeta.images.headOption
      healthy    = Either.cond(imageStorageService.objectExists(imageFile.fileName), "Healthy", "Internal server error")
    } yield healthy

    maybeHealth.getOrElse(Right("Healthy"))
  }

  private def randomImage: Option[ImageMetaInformation] = imageRepository.getRandomImage() match {
    case Success(meta) => meta
    case Failure(ex)   =>
      logger.error("Failed to fetch random image in health check", ex)
      None
  }
}
