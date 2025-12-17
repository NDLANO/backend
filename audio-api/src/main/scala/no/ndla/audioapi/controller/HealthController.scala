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

import scala.util.{Failure, Success, Try}

class HealthController(using
    s3Client: NDLAS3Client,
    audioRepository: AudioRepository,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ControllerErrorHandling,
) extends TapirHealthController {
  private def checkBucketAccess(): Either[String, Unit] = s3Client.canAccessBucket match {
    case Failure(ex) =>
      logger.error("Readiness failed: cannot access audio bucket", ex)
      Left("Audio storage unavailable")
    case Success(_) => Right(())
  }

  private def checkDatabaseAccess(): Either[String, Unit] = Try(audioRepository.audioCount) match {
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
