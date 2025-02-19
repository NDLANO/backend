/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import no.ndla.common.aws.NdlaS3Object
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import software.amazon.awssdk.services.transcribe.model.{
  GetTranscriptionJobResponse,
  StartTranscriptionJobResponse,
  TranscriptionJob,
  TranscriptionJobStatus
}
import scala.util.Success

class TranscriptionServiceTest extends UnitSuite with TestEnvironment {
  override val transcriptionService: TranscriptionService = new TranscriptionService
  override val brightcoveClient: NdlaBrightcoveClient     = new NdlaBrightcoveClient
  override val props: AudioApiProperties = new AudioApiProperties {
    override val BrightcoveAccountId: String    = "123"
    override val BrightcoveClientId: String     = "123"
    override val BrightcoveClientSecret: String = "123"
  }

  test("getAudioExtractionStatus returns Success when audio file exists") {
    val videoId      = "1"
    val language     = "en"
    val fakeS3Object = mock[NdlaS3Object]
    when(s3TranscribeClient.getObject(any)).thenReturn(Success(fakeS3Object))
    val result = transcriptionService.getAudioExtractionStatus(videoId, language)

    result should be(Success(()))
  }

  test("getTranscription returns status of a transcription") {
    val videoId                = "1"
    val language               = "en"
    val fakeS3Object           = mock[NdlaS3Object]
    val fakeTranscribeResponse = mock[GetTranscriptionJobResponse]
    val fakeJob                = mock[TranscriptionJob]
    val fakeJobStatus          = mock[TranscriptionJobStatus]
    when(s3TranscribeClient.getObject(any)).thenReturn(Success(fakeS3Object))

    when(fakeJob.transcriptionJobStatus()).thenReturn(fakeJobStatus)
    when(fakeTranscribeResponse.transcriptionJob()).thenReturn(fakeJob)
    when(transcribeClient.getTranscriptionJob(any)).thenReturn(Success(fakeTranscribeResponse))

    val result = transcriptionService.getVideoTranscription(videoId, language)

    result should be(Success(TranscriptionNonComplete(fakeJobStatus)))
  }

  test("transcribeVideo returns Success when transcription is started") {
    val videoId            = "1"
    val language           = "no-NO"
    val maxSpeakers        = 2
    val fakeS3Object       = mock[NdlaS3Object]
    val fakeTranscribeMock = mock[StartTranscriptionJobResponse]
    when(transcribeClient.getTranscriptionJob(any)).thenReturn(Success(mock[GetTranscriptionJobResponse]))
    when(s3TranscribeClient.getObject(any)).thenReturn(Success(fakeS3Object))
    when(transcriptionService.getAudioExtractionStatus(videoId, language)).thenReturn(Success(()))
    when(transcribeClient.startTranscriptionJob(any, any, any, any, any, any, any, any, any))
      .thenReturn(Success(fakeTranscribeMock))
    val result = transcriptionService.transcribeVideo(videoId, language, maxSpeakers)

    result should be(Success(()))
  }

}
