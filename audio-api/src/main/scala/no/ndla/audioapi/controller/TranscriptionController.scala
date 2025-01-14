/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.TranscriptionResultDTO
import no.ndla.audioapi.service.{ReadService, TranscriptionService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.AUDIO_API_WRITE
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{EndpointInput, endpoint, path}
import sttp.tapir.*
import sttp.tapir.generic.auto.schemaForCaseClass

import scala.util.{Failure, Success}
trait TranscriptionController {
  this: Props & TapirController & ReadService & TranscriptionService =>
  val transcriptionController: TranscriptionController
  class TranscriptionController() extends TapirController {

    override val serviceName: String         = "transcription"
    override val prefix: EndpointInput[Unit] = "audio-api" / "v1" / serviceName

    private val videoId   = path[String]("videoId").description("The video id to transcribe")
    private val audioName = path[String]("audioName").description("The audio name to transcribe")
    private val audioId   = path[Long]("audioId").description("The audio id to transcribe")
    private val language  = path[String]("language").description("The language to run the transcription in")
    private val maxSpeaker =
      query[Int]("maxSpeaker").description("The maximum number of speakers in the video").default(2)
    private val format = query[String]("format").description("The format of the audio file").default("mp3")

    def postExtractAudio: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Extract audio from video")
      .description("Extracts audio from a Brightcove video and uploads it to S3.")
      .in(videoId)
      .in(language)
      .in("extract-audio")
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(AUDIO_API_WRITE)
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
      .requirePermission(AUDIO_API_WRITE)
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
      .description("Transcribes a video and uploads the transcription to S3.")
      .in("video")
      .in(videoId)
      .in(language)
      .in(maxSpeaker)
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language, maxSpeakerOpt) =>
          transcriptionService.transcribeVideo(videoId, language, maxSpeakerOpt) match {
            case Success(_)  => Right(())
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    def getTranscription: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get the transcription status of a video")
      .description("Get the transcription of a video.")
      .in("video")
      .in(videoId)
      .in(language)
      .errorOut(errorOutputsFor(400, 404, 405, 500))
      .out(jsonBody[TranscriptionResultDTO])
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ =>
        { case (videoId, language) =>
          transcriptionService.getVideoTranscription(videoId, language) match {
            case Success(TranscriptionComplete(transcriptionContent)) =>
              Right(TranscriptionResultDTO("COMPLETED", Some(transcriptionContent.toString)))
            case Success(TranscriptionNonComplete(jobStatus)) =>
              Right(TranscriptionResultDTO(jobStatus.toString, None))
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    def postAudioTranscription: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Transcribe audio")
      .description("Transcribes an audiofile and uploads the transcription to S3.")
      .in("audio")
      .in(audioName)
      .in(audioId)
      .in(language)
      .in(maxSpeaker)
      .in(format)
      .errorOut(errorOutputsFor(400, 500))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ =>
        { case (audioName, audioId, language, maxSpeakerOpt, format) =>
          transcriptionService.transcribeAudio(audioName, audioId, language, maxSpeakerOpt, format) match {
            case Success(_)  => Right(())
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    def getAudioTranscription: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get the transcription status of an audiofile")
      .description("Get the transcription of an audiofile .")
      .in("audio")
      .in(audioId)
      .in(language)
      .errorOut(errorOutputsFor(400, 404, 405, 500))
      .out(jsonBody[TranscriptionResultDTO])
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ =>
        { case (audioId, language) =>
          transcriptionService.getAudioTranscription(audioId, language) match {
            case Success(Right(transcriptionContent)) =>
              Right(TranscriptionResultDTO("COMPLETED", Some(transcriptionContent)))
            case Success(Left(jobStatus)) =>
              Right(TranscriptionResultDTO(jobStatus.toString, None))
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] =
      List(
        postExtractAudio,
        getAudioExtraction,
        postTranscription,
        getTranscription,
        postAudioTranscription,
        getAudioTranscription
      )
  }

}
