/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.aws

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.transcribe.model.*
import software.amazon.awssdk.services.transcribe.{TranscribeClient, TranscribeClientBuilder}

import scala.util.{Failure, Try}

class NdlaAWSTranscribeClient(region: Option[String]) {
  lazy val client: TranscribeClient = {
    val builder: TranscribeClientBuilder = TranscribeClient.builder()
    region match {
      case Some(value) => builder.region(Region.of(value)).build()
      case None        => builder.build()
    }
  }

  def startTranscriptionJob(
      jobName: String,
      mediaUri: String,
      mediaFormat: String,
      languageCode: String,
      outputBucket: String,
      outputKey: String,
      maxSpeakers: Int,
      includeSubtitles: Boolean = true,
      outputSubtitleFormat: String = "VTT",
  ): Try[StartTranscriptionJobResponse] = Try {
    val requestBuilder = StartTranscriptionJobRequest
      .builder()
      .transcriptionJobName(jobName)
      .media(Media.builder().mediaFileUri(mediaUri).build())
      .mediaFormat(mediaFormat)
      .languageCode(languageCode)
      .outputBucketName(outputBucket)
      .outputKey(outputKey)
      .settings(Settings.builder().showSpeakerLabels(true).maxSpeakerLabels(maxSpeakers).build())

    val toBuild =
      if (includeSubtitles) {
        requestBuilder.subtitles(Subtitles.builder().formats(SubtitleFormat.valueOf(outputSubtitleFormat)).build())
      } else {
        requestBuilder
      }

    client.startTranscriptionJob(toBuild.build())
  }

  def getTranscriptionJob(jobName: String): Try[GetTranscriptionJobResponse] = {
    Try {
      val request = GetTranscriptionJobRequest.builder().transcriptionJobName(jobName).build()
      client.getTranscriptionJob(request)
    }.recoverWith { case e: BadRequestException =>
      val nfe = no.ndla.common.errors.NotFoundException("Transcription job not found")
      Failure(nfe.initCause(e))
    }
  }

  def listTranscriptionJobs(status: Option[String] = None): Try[ListTranscriptionJobsResponse] = Try {
    val requestBuilder = ListTranscriptionJobsRequest.builder()
    val request        = status match {
      case Some(jobStatus) => requestBuilder.status(jobStatus).build()
      case None            => requestBuilder.build()
    }

    client.listTranscriptionJobs(request)
  }

  def deleteTranscriptionJob(jobName: String): Try[DeleteTranscriptionJobResponse] = Try {
    val request = DeleteTranscriptionJobRequest.builder().transcriptionJobName(jobName).build()

    client.deleteTranscriptionJob(request)
  }
}
