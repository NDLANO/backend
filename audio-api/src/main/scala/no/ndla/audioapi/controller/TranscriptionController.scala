package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.service.{ReadService, TranscriptionService}
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{EndpointInput, endpoint, path}
import sttp.tapir.*

import scala.util.{Failure, Success}
trait TranscriptionController {
  this: Props & TapirController & ReadService & TranscriptionService =>
  val transcriptionController: TranscriptionController
  class TranscriptionController() extends TapirController {

    override val serviceName: String         = "transcription"
    override val prefix: EndpointInput[Unit] = "audio-api" / "v1" / serviceName

    private val videoId  = path[String]("videoId").description("The video id to transcribe")
    private val language = path[String]("language").description("The language to transcribe the video to")

    def postExtractAudio: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Extract audio from video")
      .description("Extracts audio from a Brightcove video and uploads it to S3.")
      .in(videoId)
      .in(language)
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language) =>
          transcriptionService.extractAudioFromVideo(videoId, language) match {
            case Success(_)  => Right(())
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    def getAudioExtraction: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get audio extraction status")
      .description("Get the status of the audio extraction from a Brightcove video.")
      .in(videoId)
      .in(language)
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language) =>
          transcriptionService.getAudioExtractionStatus(videoId, language) match {
            case Success(_)  => Right(())
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(postExtractAudio, getAudioExtraction)
  }

}
