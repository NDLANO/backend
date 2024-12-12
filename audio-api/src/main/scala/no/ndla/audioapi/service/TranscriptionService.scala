package no.ndla.audioapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
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

    def transcribeVideo(videoId: String, language: String): Try[Unit] = {
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

      val audioUri = s"s3://${props.TranscribeStorageName}/audio/$language/$videoId.mp3"
      logger.info(s"Transcribing audio from: $audioUri")
      val jobName      = s"transcription-$videoId-$language"
      val mediaFormat  = "mp3"
      val languageCode = language

      transcribeClient.startTranscriptionJob(jobName, audioUri, mediaFormat, languageCode) match {
        case Success(_) =>
          logger.info(s"Transcription job started for videoId: $videoId")
          Success(())
        case Failure(exception) =>
          Failure(new RuntimeException(s"Failed to start transcription for videoId: $videoId", exception))
      }
    }

    def getTranscription(videoId: String, language: String): Try[String] = {
      val jobName = s"transcription-$videoId-$language"

      transcribeClient.getTranscriptionJob(jobName).map { transcriptionJobResponse =>
        val transcriptionJobStatus = transcriptionJobResponse.transcriptionJob().transcriptionJobStatus()
        transcriptionJobStatus.toString
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
          logger.info("dasjhkdaidashjdas")
          val s3Key = s"audio/$language/$videoId.mp3"
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
      s3TranscribeClient.getObject(s"audio/$language/${videoId}.mp3") match {
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
