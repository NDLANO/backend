/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.integration.TranscribeS3Client
import no.ndla.audioapi.model.api.JobAlreadyFoundException
import no.ndla.common.aws.NdlaAWSTranscribeClient
import no.ndla.common.brightcove.NdlaBrightcoveClient
import no.ndla.common.model.domain.UploadedFile
import software.amazon.awssdk.services.transcribe.model.TranscriptionJobStatus
import sttp.client3.{HttpURLConnectionBackend, UriContext, asFile, basicRequest}
import ws.schild.jave.{Encoder, MultimediaObject}
import ws.schild.jave.encode.{AudioAttributes, EncodingAttributes}

import java.io.File
import scala.util.{Failure, Success, Try}

sealed trait TranscriptionResult
case class TranscriptionComplete(transcription: String)             extends TranscriptionResult
case class TranscriptionNonComplete(status: TranscriptionJobStatus) extends TranscriptionResult

class TranscriptionService(using
    s3TranscribeClient: => TranscribeS3Client,
    props: Props,
    brightcoveClient: NdlaBrightcoveClient,
    transcribeClient: => NdlaAWSTranscribeClient
) extends StrictLogging {

  def transcribeVideo(videoId: String, language: String, maxSpeakers: Int): Try[Unit] = {
    getVideoTranscription(videoId, language) match {
      case Success(TranscriptionComplete(_)) =>
        logger.info(s"Transcription already completed for videoId: $videoId")
        return Failure(JobAlreadyFoundException(s"Transcription already completed for videoId: $videoId"))
      case Success(TranscriptionNonComplete(TranscriptionJobStatus.IN_PROGRESS)) =>
        logger.info(s"Transcription already in progress for videoId: $videoId")
        return Failure(JobAlreadyFoundException(s"Transcription already in progress for videoId: $videoId"))
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
    val jobName      = s"transcribe-video-$videoId-$language"
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
  ): Try[TranscriptionResult] = {
    val jobName = s"transcribe-video-$videoId-$language"

    transcribeClient.getTranscriptionJob(jobName).flatMap { transcriptionJobResponse =>
      val transcriptionJob       = transcriptionJobResponse.transcriptionJob()
      val transcriptionJobStatus = transcriptionJob.transcriptionJobStatus()

      if (transcriptionJobStatus == TranscriptionJobStatus.COMPLETED) {
        val transcribeUri = s"transcription/$language/$videoId.vtt"

        getObjectFromS3(transcribeUri).map(TranscriptionComplete(_))
      } else {
        Success(TranscriptionNonComplete(transcriptionJobStatus))
      }
    }
  }

  def transcribeAudio(
      audioName: String,
      audioId: Long,
      language: String,
      maxSpeakers: Int,
      format: String
  ): Try[Unit] = {
    getAudioTranscription(audioId, language) match {
      case Success(Right(_)) =>
        logger.info(s"Transcription already completed for audio: $audioName")
        return Failure(JobAlreadyFoundException(s"Transcription already completed for audio: $audioName"))
      case Success(Left("IN_PROGRESS")) =>
        logger.info(s"Transcription already in progress for videoId: $audioName")
        return Failure(JobAlreadyFoundException(s"Transcription already in progress for audio: $audioName"))
      case _ =>
        logger.info(s"No existing transcription job for audio name: $audioName")
    }
    val audioUri = s"s3://${props.StorageName}/$audioName"
    logger.info(s"Transcribing audio from: $audioUri")
    val jobName      = s"transcribe-audio-$audioId-$language"
    val mediaFormat  = format
    val outputKey    = s"audio-transcription/$language/$audioId"
    val languageCode = language
    transcribeClient.startTranscriptionJob(
      jobName,
      audioUri,
      mediaFormat,
      languageCode,
      props.TranscribeStorageName,
      outputKey,
      maxSpeakers,
      includeSubtitles = false
    ) match {
      case Success(_) =>
        logger.info(s"Transcription job started for audio: $audioName")
        Success(())
      case Failure(exception) =>
        Failure(new RuntimeException(s"Failed to start transcription for audio file: $audioName", exception))
    }
  }

  def getAudioTranscription(audioId: Long, language: String): Try[Either[String, String]] = {
    val jobName = s"transcribe-audio-$audioId-$language"

    transcribeClient.getTranscriptionJob(jobName).flatMap { transcriptionJobResponse =>
      val transcriptionJob       = transcriptionJobResponse.transcriptionJob()
      val transcriptionJobStatus = transcriptionJob.transcriptionJobStatus().toString

      if (transcriptionJobStatus == "COMPLETED") {
        val transcribeUri = s"audio-transcription/$language/$audioId"

        getObjectFromS3(transcribeUri).map(Right(_))
      } else {
        Success(Left(transcriptionJobStatus))
      }
    }
  }

  private def getObjectFromS3(Uri: String): Try[String] = {
    s3TranscribeClient.getObject(Uri).map { s3Object =>
      val content = scala.io.Source.fromInputStream(s3Object.stream).mkString
      s3Object.stream.close()
      content
    }
  }

  def extractAudioFromVideo(videoId: String, language: String): Try[Unit] = {
    val accountId = props.BrightcoveAccountId
    val videoUrl  = getVideo(accountId, videoId) match {
      case Success(sources) if sources.nonEmpty => sources.head
      case Success(_)  => return Failure(new RuntimeException(s"No video sources found for videoId: $videoId"))
      case Failure(ex) => return Failure(new RuntimeException(s"Failed to get video sources: $ex"))
    }
    val videoFile = downloadVideo(videoId, videoUrl) match {
      case Success(file) => file
      case Failure(ex)   => throw new RuntimeException(s"Failed to download video: $ex")
    }

    val audioFile = new File(s"/tmp/audio_$videoId.mp3")

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
        val uploadedFile = UploadedFile( // convert to uploadedFile object
          partName = "",
          fileName = Some(s"audio_$videoId.mp3"),
          fileSize = audioFile.length(),
          contentType = Some("audio/mpeg"),
          file = audioFile
        )
        s3TranscribeClient.putObject(s3Key, uploadedFile) match {
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
    s3TranscribeClient.getObject(s"audio-extraction/$language/$videoId.mp3") match {
      case Success(_)         => Success(())
      case Failure(exception) => Failure(exception)
    }
  }

  private def getVideo(accountId: String, videoId: String): Try[Vector[String]] = {
    val clientId     = props.BrightcoveClientId
    val clientSecret = props.BrightcoveClientSecret

    for {
      token   <- brightcoveClient.getToken(clientId, clientSecret)
      sources <- brightcoveClient.getVideoSource(accountId, videoId, token)
      mp4Sources = sources
        .filter(source => source.hcursor.get[String]("container").toOption.contains("MP4"))
        .map(source => source.hcursor.get[String]("src").toOption.getOrElse(""))
      result <-
        if (mp4Sources.nonEmpty) Success(mp4Sources) else Failure(new RuntimeException("No MP4 sources found"))
    } yield result
  }

  private def downloadVideo(videoId: String, videoUrl: String): Try[File] = {
    val videoFile  = new File(s"/tmp/video_$videoId.mp4")
    val connection = HttpURLConnectionBackend()

    val response = basicRequest.get(uri"$videoUrl").response(asFile(videoFile)).send(connection)
    Try {
      response.body match {
        case Right(file) => file
        case Left(error) => throw new RuntimeException(s"Failed to download video: $error")
      }
    } match {
      case Success(file)      => Success(file)
      case Failure(exception) => Failure(exception)
    }

  }
}
