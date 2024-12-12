/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.aws

import software.amazon.awssdk.services.transcribe.{TranscribeClient, TranscribeClientBuilder}
import software.amazon.awssdk.services.transcribe.model.*

import scala.util.{Failure, Try}

trait NdlaAWSTranscribeClient {
  val transcribeClient: NdlaAWSTranscribeClient

  class NdlaAWSTranscribeClient(region: Option[String]) {

    private val builder: TranscribeClientBuilder = TranscribeClient.builder()

    val client: TranscribeClient = region match {
      case Some(value) => builder.region(software.amazon.awssdk.regions.Region.of(value)).build()
      case None        => builder.build()
    }

    def startTranscriptionJob(
        jobName: String,
        mediaUri: String,
        mediaFormat: String,
        languageCode: String
    ): Try[StartTranscriptionJobResponse] = Try {
      val request = StartTranscriptionJobRequest
        .builder()
        .transcriptionJobName(jobName)
        .media(Media.builder().mediaFileUri(mediaUri).build())
        .mediaFormat(mediaFormat)
        .languageCode(languageCode)
        .build()

      client.startTranscriptionJob(request)
    }

    def getTranscriptionJob(jobName: String): Try[GetTranscriptionJobResponse] = {
      Try {
        val request = GetTranscriptionJobRequest
          .builder()
          .transcriptionJobName(jobName)
          .build()
        client.getTranscriptionJob(request)
      }.recoverWith { case e: BadRequestException =>
        val nfe = no.ndla.common.errors.NotFoundException("Transcription job not found")
        Failure(nfe.initCause(e))
      }
    }

    def listTranscriptionJobs(status: Option[String] = None): Try[ListTranscriptionJobsResponse] = Try {
      val requestBuilder = ListTranscriptionJobsRequest.builder()
      val request = status match {
        case Some(jobStatus) => requestBuilder.status(jobStatus).build()
        case None            => requestBuilder.build()
      }

      client.listTranscriptionJobs(request)
    }

    def deleteTranscriptionJob(jobName: String): Try[DeleteTranscriptionJobResponse] = Try {
      val request = DeleteTranscriptionJobRequest
        .builder()
        .transcriptionJobName(jobName)
        .build()

      client.deleteTranscriptionJob(request)
    }
  }
}
