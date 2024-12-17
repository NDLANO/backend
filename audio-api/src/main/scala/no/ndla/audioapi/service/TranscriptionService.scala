package no.ndla.audioapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.JobAlreadyFoundException
import no.ndla.common.aws.{NdlaAWSTranscribeClient, NdlaS3Client}
import no.ndla.common.brightcove.NdlaBrightcoveClient
import sttp.client3.{HttpURLConnectionBackend, UriContext, asFile, basicRequest}
import ws.schild.jave.{Encoder, MultimediaObject}
import ws.schild.jave.encode.{AudioAttributes, EncodingAttributes}

import java.io.File
import scala.util.{Failure, Success, Try}

trait TranscriptionService {
  this: NdlaS3Client & Props & NdlaBrightcoveClient & NdlaAWSTranscribeClient =>
  val transcriptionService: TranscriptionService
  val s3TranscribeClient: NdlaS3Client

  class TranscriptionService extends StrictLogging {

    def transcribeVideo(videoId: String, language: String, maxSpeakers: Int): Try[Unit] = {
      getVideoTranscription(videoId, language) match {
        case Success(Right(_)) =>
          logger.info(s"Transcription already completed for videoId: $videoId")
          return Failure(new JobAlreadyFoundException(s"Transcription already completed for videoId: $videoId"))
        case Success(Left("IN_PROGRESS")) =>
          logger.info(s"Transcription already in progress for videoId: $videoId")
          return Failure(new JobAlreadyFoundException(s"Transcription already in progress for videoId: $videoId"))
        case Success(Left(_)) =>
          logger.info(s"Error occurred while checking transcription status for videoId")
        case _ =>
          logger.info(s"No existing transcription job for videoId: $videoId")
      }

      getAudioExtractionStatus(videoId, language) match {
        case Success(_) =>
          logger.info(s"Audio already extracted for videoId: $videoId")
        case Failure(_) =>
          logger.info(s"Audio extraction required for videoId: $videoId")
          extractAudioFromVideo(videoId, language) match {
            case Success(_) =>
              logger.info(s"Audio extracted for videoId: $videoId")
            case Failure(exception) =>
              return Failure(new RuntimeException(s"Failed to extract audio for videoId: $videoId", exception))

          }
      }

      val audioUri = s"s3://${props.TranscribeStorageName}/audio-extraction/$language/$videoId.mp3"
      logger.info(s"Transcribing audio from: $audioUri")
      val jobName      = s"transcribe-$videoId-$language"
      val mediaFormat  = "mp3"
      val outputKey    = s"transcription/$language/$videoId"
      val languageCode = language

      transcribeClient.startTranscriptionJob(
        jobName,
        audioUri,
        mediaFormat,
        languageCode,
        props.TranscribeStorageName,
        outputKey,
        maxSpeakers
      ) match {
        case Success(_) =>
          logger.info(s"Transcription job started for videoId: $videoId")
          Success(())
        case Failure(exception) =>
          Failure(new RuntimeException(s"Failed to start transcription for videoId: $videoId", exception))
      }
    }

    def getVideoTranscription(
        videoId: String,
        language: String
    ): Try[Either[String, String]] = {
      val jobName = s"transcribe-$videoId-$language"

      transcribeClient.getTranscriptionJob(jobName).flatMap { transcriptionJobResponse =>
        val transcriptionJob       = transcriptionJobResponse.transcriptionJob()
        val transcriptionJobStatus = transcriptionJob.transcriptionJobStatus().toString

        if (transcriptionJobStatus == "COMPLETED") {
          val transcribeUri = s"transcription/$language/${videoId}.vtt"

          s3TranscribeClient.getObject(transcribeUri).map { s3Object =>
            val content = scala.io.Source.fromInputStream(s3Object.stream).mkString
            s3Object.stream.close()
            Right(content)
          }
        } else {
          Success(Left(transcriptionJobStatus))
        }
      }
    }

    def transcribeAudio(audioName: String, language: String, maxSpeakers: Int, format: String): Try[Unit] = {
      getVideoTranscription(audioName, language) match {
        case Success(Right(_)) =>
          logger.info(s"Transcription already completed for audio: $audioName")
          return Failure(new JobAlreadyFoundException(s"Transcription already completed for audio: $audioName"))
        case Success(Left("IN_PROGRESS")) =>
          logger.info(s"Transcription already in progress for videoId: $audioName")
          return Failure(new JobAlreadyFoundException(s"Transcription already in progress for audio: $audioName"))
        case Success(Left(_)) =>
          logger.info(s"Error occurred while checking transcription status for audio")
        case _ =>
          logger.info(s"No existing transcription job for audio name: $audioName")
      }
      val audioUri = s"s3://${props.StorageName}/$audioName.mp3"
      logger.info(s"Transcribing audio from: $audioUri")
      val jobName      = s"transcribe-$audioName-$language"
      val mediaFormat  = format
      val outputKey    = s"audio-transcription/$language/$audioName"
      val languageCode = language

      transcribeClient.startTranscriptionJob(
        jobName,
        audioUri,
        mediaFormat,
        languageCode,
        props.TranscribeStorageName,
        outputKey,
        maxSpeakers
      ) match {
        case Success(_) =>
          logger.info(s"Transcription job started for audio: $audioName")
          Success(())
        case Failure(exception) =>
          Failure(new RuntimeException(s"Failed to start transcription for audio file: $audioName", exception))
      }
    }

    def getAudioTranscription(audioName: String, language: String): Try[Either[String, String]] = {
      val jobName = s"transcribe-$audioName-$language"

      transcribeClient.getTranscriptionJob(jobName).flatMap { transcriptionJobResponse =>
        val transcriptionJob       = transcriptionJobResponse.transcriptionJob()
        val transcriptionJobStatus = transcriptionJob.transcriptionJobStatus().toString

        if (transcriptionJobStatus == "COMPLETED") {
          val transcribeUri = s"audio-transcription/$language/${audioName}"

          s3TranscribeClient.getObject(transcribeUri).map { s3Object =>
            val content = scala.io.Source.fromInputStream(s3Object.stream).mkString
            s3Object.stream.close()
            Right(content)
          }
        } else {
          Success(Left(transcriptionJobStatus))
        }
      }
    }
    def extractAudioFromVideo(videoId: String, language: String): Try[Unit] = {
      val accountId = props.BrightcoveAccountId
      val videoUrl = getVideo(accountId, videoId) match {
        case Right(sources) => sources.head
        case Left(error)    => throw new RuntimeException(s"Failed to get video sources: $error")
      }
      val videoFile = downloadVideo(videoId, videoUrl)

      val audioFile = new File(s"/tmp/audio_${videoId}.mp3")

      val audioAttributes = new AudioAttributes()
      audioAttributes.setCodec("libmp3lame")
      audioAttributes.setBitRate(128000)
      audioAttributes.setChannels(2)
      audioAttributes.setSamplingRate(44100)

      val encodingAttributes = new EncodingAttributes()
      encodingAttributes.setOutputFormat("mp3")
      encodingAttributes.setAudioAttributes(audioAttributes)

      val encoder = new Encoder()
      Try {
        encoder.encode(new MultimediaObject(videoFile), audioFile, encodingAttributes)
      } match {
        case Success(_) =>
          val s3Key = s"audio-extraction/$language/$videoId.mp3"
          logger.info(s"Uploading audio file to S3: $s3Key")
          s3TranscribeClient.putObject(s3Key, audioFile, "audio/mpeg") match {
            case Success(_) =>
              logger.info(s"Audio file uploaded to S3: $s3Key")
              for {
                _ <- Try(audioFile.delete())
                _ <- Try(videoFile.delete())
              } yield ()
            case Failure(ex) =>
              Failure(new RuntimeException(s"Failed to upload audio file to S3.", ex))
          }
        case Failure(exception) => Failure(exception)
      }
    }

    def getAudioExtractionStatus(videoId: String, language: String): Try[Unit] = {
      s3TranscribeClient.getObject(s"audio-extraction/$language/${videoId}.mp3") match {
        case Success(_)         => Success(())
        case Failure(exception) => Failure(exception)
      }
    }

    private def getVideo(accountId: String, videoId: String): Either[String, Vector[String]] = {
      val clientId     = props.BrightcoveClientId
      val clientSecret = props.BrightcoveClientSecret
      val token        = brightcoveClient.getToken(clientId, clientSecret)
      token match {
        case Right(bearerToken) =>
          val cake = brightcoveClient.getVideoSource(accountId, videoId, bearerToken)
          cake match {
            case Right(videoSources) =>
              val mp4Sources = videoSources
                .filter(source => source.hcursor.get[String]("container").toOption.contains("MP4"))
                .map(source => source.hcursor.get[String]("src").toOption.getOrElse(""))
              if (mp4Sources.nonEmpty) Right(mp4Sources)
              else Left("No MP4 sources found for video.")
            case Left(error) => Left(s"Failed to get video sources: $error")
          }
        case Left(error) =>
          Left(s"Failed to retrieve bearer token: $error")
      }
    }

    private def downloadVideo(videoId: String, videoUrl: String): File = {
      val videoFile  = new File(s"/tmp/video_$videoId.mp4")
      val connection = HttpURLConnectionBackend()

      val response = basicRequest.get(uri"$videoUrl").response(asFile(videoFile)).send(connection)

      response.body match {
        case Right(file) => file
        case Left(error) => throw new RuntimeException(s"Failed to download video: $error")
      }
    }
  }
}
