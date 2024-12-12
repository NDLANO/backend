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
      .in("extract-audio")
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
      .in("extract-audio")
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

    def postTranscription: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Transcribe video")
      .description("Transcribes a video to a specific language, and uploads the transcription to S3.")
      .in(videoId)
      .in(language)
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language) =>
          transcriptionService.transcribeVideo(videoId, language) match {
            case Success(_)  => Right(())
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    def getTranscription: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get transcription")
      .description("Get the transcription of a video.")
      .in(videoId)
      .in(language)
      .errorOut(errorOutputsFor(400, 404, 500))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language) =>
          transcriptionService.getTranscription(videoId, language) match {
            case Success(_)                          => Right(())
            case Failure(ex: NoSuchElementException) => returnLeftError(ex)
            case Failure(ex)                         => returnLeftError(ex)
          }
        }
      }

    /*def postTranscriptionFormatting: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Format transcription")
      .description("Formats a transcription to a specific format.")
      .in(videoId)
      .in(language)
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language) =>
          transcriptionService.formatTranscription(videoId, language) match {
            case Success(_)  => Right(())
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }*/

    override val endpoints: List[ServerEndpoint[Any, Eff]] =
      List(postExtractAudio, getAudioExtraction, postTranscription, getTranscription)
  }

}
