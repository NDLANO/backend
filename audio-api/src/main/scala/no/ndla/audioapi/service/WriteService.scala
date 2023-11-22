/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.model.api.{AudioStorageException, MissingIdException, NotFoundException}
import no.ndla.audioapi.model.domain.Audio
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service.search.{AudioIndexService, SeriesIndexService, TagIndexService}
import no.ndla.common.Clock
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.domain.UploadedFile
import no.ndla.common.model.{domain => common}
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.network.tapir.auth.TokenUser

import java.lang.Math.max
import scala.util.{Failure, Random, Success, Try}

trait WriteService {
  this: ConverterService
    with ValidationService
    with AudioRepository
    with SeriesRepository
    with AudioIndexService
    with SeriesIndexService
    with TagIndexService
    with AudioStorageService
    with ReadService
    with Clock =>
  val writeService: WriteService

  class WriteService extends StrictLogging {

    def updateSeries(id: Long, toUpdateSeries: api.NewSeries): Try[api.Series] = {
      seriesRepository.withId(id) match {
        case Failure(ex)   => Failure(ex)
        case Success(None) => Failure(new NotFoundException(s"Could not find series to update with id: '$id'"))
        case Success(Some(existingSeries)) =>
          val merged             = converterService.updateSeries(existingSeries, toUpdateSeries)
          val oldEpisodesIds     = existingSeries.episodes.traverse(_.flatMap(_.id)).flatten.toSet
          val episodesToDelete   = oldEpisodesIds.diff(toUpdateSeries.episodes)
          val episodesToAdd      = toUpdateSeries.episodes.diff(oldEpisodesIds)
          val episodesToValidate = toUpdateSeries.episodes.map(id => id -> audioRepository.withId(id))

          for {
            validatedEpisodes <- validationService.validatePodcastEpisodes(
              episodesToValidate.toSeq,
              Some(existingSeries.id)
            )
            validatedSeries <- validationService.validate(merged)
            updatedSeries   <- seriesRepository.update(validatedSeries)
            _               <- updateSeriesForEpisodes(None, episodesToDelete.toSeq)
            _               <- updateSeriesForEpisodes(Some(validatedSeries.id), episodesToAdd.toSeq)
            updatedWithEpisodes = updatedSeries.copy(episodes = Some(validatedEpisodes))
            _         <- seriesIndexService.indexDocument(updatedWithEpisodes)
            converted <- converterService.toApiSeries(updatedWithEpisodes, Some(toUpdateSeries.language))
          } yield converted

      }
    }

    def newSeries(newSeries: api.NewSeries): Try[api.Series] = {
      val domainSeries = converterService.toDomainSeries(newSeries)
      val episodes     = newSeries.episodes.map(id => id -> audioRepository.withId(id))

      for {
        validatedEpisodes <- validationService.validatePodcastEpisodes(episodes.toSeq, None)
        validatedSeries   <- validationService.validate(domainSeries)
        inserted          <- seriesRepository.insert(validatedSeries)
        _                 <- updateSeriesForEpisodes(Some(inserted.id), newSeries.episodes.toSeq)
        insertedWithEpisodes = inserted.copy(episodes = Some(validatedEpisodes))
        _         <- seriesIndexService.indexDocument(insertedWithEpisodes)
        converted <- converterService.toApiSeries(insertedWithEpisodes, Some(newSeries.language))
      } yield converted

    }

    def updateSeriesForEpisodes(seriesId: Option[Long], episodeIds: Seq[Long]): Try[_] =
      episodeIds.traverse(id =>
        for {
          _ <- audioRepository.setSeriesId(
            audioMetaId = id,
            seriesId = seriesId
          )
          episode <- audioRepository.withId(id) match {
            case Some(ep) => Success(ep)
            case None =>
              Failure(new NotFoundException(s"Could not find episode with id '$id' when updating series connection."))
          }
          reindexed <- audioIndexService.indexDocument(episode)
        } yield reindexed
      )

    def deleteSeries(seriesId: Long): Try[Long] = {
      seriesRepository.withId(seriesId) match {
        case Failure(ex) => Failure(ex)
        case Success(None) =>
          Failure(new NotFoundException(s"Series with id $seriesId was not found, and could not be deleted."))
        case Success(Some(existingSeries)) =>
          val episodes = existingSeries.episodes.getOrElse(Seq.empty)
          freeEpisodes(episodes) match {
            case Failure(ex) => Failure(ex)
            case Success(_) =>
              seriesRepository.deleteWithId(seriesId) match {
                case Success(numRows) if numRows > 0 => seriesIndexService.deleteDocument(seriesId)
                case Success(_) =>
                  Failure(new NotFoundException(s"Could not find series to delete with id: '$seriesId'"))
                case Failure(ex) => Failure(ex)
              }
          }
      }
    }

    private def idToTry(id: Option[Long]): Try[Long] = {
      id match {
        case Some(foundId) => Success(foundId)
        case None          => Failure(MissingIdException("Id not found, this is likely a bug."))
      }
    }

    private def freeEpisodes(episodes: Seq[domain.AudioMetaInformation]): Try[Seq[domain.AudioMetaInformation]] =
      episodes.traverse(ep => {
        for {
          id        <- idToTry(ep.id)
          _         <- audioRepository.setSeriesId(id, None)
          reindexed <- audioIndexService.indexDocument(ep.copy(series = None, seriesId = None))
        } yield reindexed
      })

    def deleteAudioLanguageVersion(audioId: Long, language: String): Try[Option[api.AudioMetaInformation]] =
      audioRepository.withId(audioId) match {
        case Some(existing) if existing.supportedLanguages.contains(language) =>
          val newAudio = converterService.withoutLanguage(existing, language)

          // If last language version delete entire audio
          if (newAudio.supportedLanguages.isEmpty)
            deleteAudioAndFiles(audioId).map(_ => None)
          else {
            val removedFilePath = existing.filePaths.find(audio => audio.language == language).get
            // If last audio with this filePath, delete the file.
            val deleteResult = if (!newAudio.filePaths.exists(audio => audio.filePath == removedFilePath.filePath)) {
              deleteFile(removedFilePath)
            } else Success(())

            deleteResult.flatMap(_ =>
              validateAndUpdateMetaData(audioId, newAudio, existing, None, existing.seriesId).map(Some(_))
            )

          }

        case Some(_) =>
          Failure(new NotFoundException(s"Audio with id $audioId does not exist in language '$language'."))
        case None =>
          Failure(new NotFoundException(s"Audio with id $audioId was not found, and could not be deleted."))
      }

    def deleteSeriesLanguageVersion(seriesId: Long, language: String): Try[Option[api.Series]] = {
      seriesRepository.withId(seriesId) match {
        case Success(Some(existing)) if existing.supportedLanguages.contains(language) =>
          val newSeries = converterService.withoutLanguage(existing, language)

          // If last language version delete entire series
          if (newSeries.supportedLanguages.isEmpty)
            deleteSeries(seriesId).map(_ => None)
          else {
            for {
              validated <- validationService.validate(newSeries)
              updated   <- seriesRepository.update(validated)
              indexed   <- seriesIndexService.indexDocument(updated)
              converted <- converterService.toApiSeries(indexed, None)
              result = Some(converted)
            } yield result
          }
        case Success(Some(_)) =>
          Failure(new NotFoundException(s"Series with id $seriesId does not exist in language '$language'."))
        case Success(None) =>
          Failure(new NotFoundException(s"Series with id $seriesId was not found, and could not be deleted."))
        case Failure(ex) => Failure(ex)
      }
    }

    /** Helper function to easier get series from repository with an id with type `Option[Long]` */
    private def getSeriesFromOpt(id: Option[Long], includeEpisodes: Boolean = true): Try[Option[domain.Series]] = {
      id.traverse(sId => seriesRepository.withId(sId, includeEpisodes)).map(_.flatten)
    }

    def storeNewAudio(
        newAudioMeta: api.NewAudioMetaInformation,
        file: UploadedFile,
        tokenUser: TokenUser
    ): Try[api.AudioMetaInformation] = {
      validationService.validateAudioFile(file) match {
        case msgs if msgs.nonEmpty => Failure(new ValidationException(errors = msgs))
        case _ =>
          val audioFileMeta = uploadFile(file, newAudioMeta.language) match {
            case Failure(e)         => return Failure(e)
            case Success(audioMeta) => audioMeta
          }

          val audioMetaInformation = for {
            maybeSeries <- getSeriesFromOpt(newAudioMeta.seriesId)
            domainAudio <- Try(
              converterService.toDomainAudioMetaInformation(newAudioMeta, audioFileMeta, maybeSeries, tokenUser)
            )

            _ <- validationService.validate(domainAudio, None, maybeSeries, Some(newAudioMeta.language))

            audioMetaData <- Try(audioRepository.insert(domainAudio))
            insertedId    <- idToTry(audioMetaData.id)
            _             <- audioRepository.setSeriesId(insertedId, newAudioMeta.seriesId)

            seriesToIndex <- getSeriesFromOpt(newAudioMeta.seriesId)
            _             <- seriesToIndex.traverse(series => seriesIndexService.indexDocument(series))
            _             <- audioIndexService.indexDocument(audioMetaData)
            _             <- tagIndexService.indexDocument(audioMetaData)
            converted     <- converterService.toApiAudioMetaInformation(audioMetaData, Some(newAudioMeta.language))
          } yield converted

          if (audioMetaInformation.isFailure) {
            val deletion = deleteFile(audioFileMeta)
            deletion.flatMap(_ => audioMetaInformation)
          } else {
            audioMetaInformation
          }
      }
    }

    def deleteAudioAndFiles(audioId: Long): Try[Long] = {
      audioRepository
        .withId(audioId) match {
        case Some(toDelete) =>
          val metaDeleted = audioRepository.deleteAudio(audioId)
          val filesDeleted = toDelete.filePaths.map(fileToDelete => {
            deleteFile(fileToDelete) match {
              case Failure(ex) =>
                Failure(
                  new AudioStorageException(
                    s"Deletion of file at '${fileToDelete.filePath}' failed with: ${ex.getMessage}"
                  )
                )
              case ok => ok
            }
          })
          val indexDeleted = audioIndexService.deleteDocument(audioId)

          if (metaDeleted < 1) {
            Failure(
              new NotFoundException(s"Metadata for audio with id $audioId was not found, and could not be deleted.")
            )
          } else if (filesDeleted.exists(_.isFailure)) {
            val exceptions = filesDeleted.collect { case Failure(ex) => ex }
            val msg        = exceptions.map(_.getMessage).mkString("\n")
            Failure(new AudioStorageException(msg))
          } else {
            indexDeleted match {
              case Failure(ex)        => Failure(ex)
              case Success(deletedId) => Success(deletedId)
            }
          }

        case None => Failure(new NotFoundException(s"Audio with id $audioId was not found, and could not be deleted."))
      }
    }

    def updateAudio(
        id: Long,
        metadataToUpdate: api.UpdatedAudioMetaInformation,
        fileOpt: Option[UploadedFile],
        user: TokenUser
    ): Try[api.AudioMetaInformation] = {
      audioRepository.withId(id) match {
        case None => Failure(new NotFoundException)
        case Some(existingMetadata) =>
          val metadataAndFile = fileOpt match {
            case None => mergeAudioMeta(existingMetadata, metadataToUpdate, None, user)
            case Some(file) =>
              val validationMessages = validationService.validateAudioFile(file)
              if (validationMessages.nonEmpty) {
                return Failure(new ValidationException(errors = validationMessages))
              }

              uploadFile(file, metadataToUpdate.language) match {
                case Failure(err) => Failure(err)
                case Success(uploadedFile) =>
                  mergeAudioMeta(existingMetadata, metadataToUpdate, Some(uploadedFile), user)
              }
          }

          val savedAudio     = metadataAndFile.map(_._2)
          val metadataToSave = metadataAndFile.map(_._1)

          val finished =
            metadataToSave.flatMap(
              validateAndUpdateMetaData(
                id,
                _,
                existingMetadata,
                Some(metadataToUpdate.language),
                metadataToUpdate.seriesId
              )
            )

          val deleteResult = savedAudio match {
            case Success(None)                              => Success(()) // No file, do nothing
            case Success(Some(audio)) if finished.isFailure => deleteFile(audio)
            case Success(Some(_))                           =>
              // If old file in update language version is no longer in use, delete it
              val oldAudio = existingMetadata.filePaths.find(audio => audio.language == metadataToUpdate.language)
              oldAudio match {
                case None => Success(())
                case Some(old) =>
                  if (
                    !existingMetadata.filePaths
                      .exists(audio => audio.language != old.language && audio.filePath == old.filePath)
                  ) {
                    deleteFile(old)
                  } else Success(())
              }
            case Failure(ex) => Failure(ex)
          }

          deleteResult.flatMap(_ => finished)
      }
    }

    private def validateAndUpdateMetaData(
        audioId: Long,
        toSave: domain.AudioMetaInformation,
        oldAudio: domain.AudioMetaInformation,
        language: Option[String],
        seriesId: Option[Long]
    ): Try[api.AudioMetaInformation] = {
      for {
        maybeSeries <- getSeriesFromOpt(seriesId)
        validated   <- validationService.validate(toSave, Some(oldAudio), maybeSeries, language)
        updated     <- audioRepository.update(validated, audioId)

        _         <- audioRepository.setSeriesId(audioId, seriesId)
        newSeries <- getSeriesFromOpt(seriesId)
        seriesToIndex = newSeries.orElse(oldAudio.series)
        _ <- seriesToIndex.traverse(seriesIndexService.indexDocument)

        indexed <- audioIndexService.indexDocument(updated)
        _       <- tagIndexService.indexDocument(updated)

        converted <- converterService.toApiAudioMetaInformation(indexed, language)
      } yield converted
    }

    private[service] def mergeAudioMeta(
        existing: domain.AudioMetaInformation,
        toUpdate: api.UpdatedAudioMetaInformation,
        savedAudio: Option[Audio],
        user: TokenUser
    ): Try[(domain.AudioMetaInformation, Option[Audio])] = {
      val mergedFilePaths = savedAudio match {
        // If no audio is uploaded, and the language doesn't have a filePath. Clone a prioritized one.
        case None if existing.filePaths.forall(_.language != toUpdate.language) =>
          val maybeNewFilePath = findByLanguageOrBestEffort(existing.filePaths, toUpdate.language)
            .map(_.copy(language = toUpdate.language))
          converterService.mergeLanguageField(existing.filePaths, maybeNewFilePath, toUpdate.language)
        case None => existing.filePaths
        case Some(audio) =>
          converterService.mergeLanguageField(
            existing.filePaths,
            domain.Audio(audio.filePath, audio.mimeType, audio.fileSize, audio.language)
          )

      }

      val newPodcastMeta =
        toUpdate.podcastMeta.map(meta => converterService.toDomainPodcastMeta(meta, toUpdate.language))

      val newManuscript = toUpdate.manuscript.map(manu => converterService.toDomainManuscript(manu, toUpdate.language))

      // Fetch series if its new so we can display it in body of response
      val newSeries = if (existing.seriesId != toUpdate.seriesId) {
        getSeriesFromOpt(toUpdate.seriesId, includeEpisodes = false)
      } else { Success(existing.series) }

      newSeries.map(series => {
        val merged = domain.AudioMetaInformation(
          id = existing.id,
          revision = Some(toUpdate.revision),
          titles =
            converterService.mergeLanguageField(existing.titles, common.Title(toUpdate.title, toUpdate.language)),
          tags =
            converterService.mergeLanguageField(existing.tags, common.Tag(toUpdate.tags.distinct, toUpdate.language)),
          filePaths = mergedFilePaths,
          copyright = converterService.toDomainCopyright(toUpdate.copyright),
          updated = clock.now(),
          created = existing.created,
          updatedBy = user.id,
          podcastMeta = converterService.mergeLanguageField(existing.podcastMeta, newPodcastMeta, toUpdate.language),
          manuscript = converterService.mergeLanguageField(existing.manuscript, newManuscript, toUpdate.language),
          series = series,
          seriesId = toUpdate.seriesId,
          audioType = existing.audioType
        )

        (merged, savedAudio)
      })
    }

    private[service] def deleteFile(audioFile: Audio): Try[Unit] = {
      audioStorage.deleteObject(audioFile.filePath)
    }

    private[service] def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.replace("\"", "").substring(index))
        case _                        => None
      }
    }

    private[service] def uploadFile(file: UploadedFile, language: String): Try[Audio] = {
      val fileExtension = file.fileName.flatMap(getFileExtension).getOrElse("")
      val contentType   = file.contentType.getOrElse("")
      val fileName      = LazyList.continually(randomFileName(fileExtension)).dropWhile(audioStorage.objectExists).head

      audioStorage
        .storeAudio(file, contentType, fileName)
        .map(objectMeta => Audio(fileName, objectMeta.getContentType, objectMeta.getContentLength, language))
    }

    private[service] def randomFileName(extension: String, length: Int = 12): String = {
      val extensionWithDot = if (extension.head == '.') extension else s".$extension"
      val randomString     = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

  }
}
