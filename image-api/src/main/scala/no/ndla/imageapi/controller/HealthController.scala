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

import scala.util.{Failure, Success}

class HealthController(using
    imageStorageService: ImageStorageService,
    imageRepository: ImageRepository,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirHealthController {
  private def checkBucketAccess(): Either[String, Unit] = imageStorageService.checkBucketAccess() match {
    case Failure(ex) =>
      logger.error("Readiness failed: cannot access image bucket", ex)
      Left("Image storage unavailable")
    case Success(_) => Right(())
  }

  private def checkDatabaseAccess(): Either[String, Unit] = imageRepository.imageCount match {
    case Failure(ex) =>
      logger.error("Readiness failed: cannot reach database", ex)
      Left("Database unavailable")
    case Success(_) => Right(())
  }

  override def checkReadiness(): Either[String, String] = for {
    _ <- checkDatabaseAccess()
    _ <- checkBucketAccess()
  } yield "Ready"
}
