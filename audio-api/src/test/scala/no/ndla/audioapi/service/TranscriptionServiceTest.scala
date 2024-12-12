package no.ndla.audioapi.service

import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import no.ndla.common.aws.NdlaS3Object
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.{Failure, Success, Try}

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
}
