package no.ndla.audioapi.service

import java.io.ByteArrayInputStream

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.{Audio, LanguageField}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.SearchIndexService
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Random, Success, Try}
import java.lang.Math.max

import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.model.domain

trait WriteService {
  this: ConverterService
    with ValidationService
    with AudioRepository
    with SearchIndexService
    with AudioStorageService
    with Clock
    with User =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def deleteAudioLanguageVersion(audioId: Long, language: String) =
      audioRepository.withId(audioId) match {
        case Some(existing) if existing.supportedLanguages.contains(language) =>
          val newAudio = converterService.withoutLanguage(existing, language)

          // If last language version delete entire audio
          if (newAudio.supportedLanguages.isEmpty)
            deleteAudioAndFiles(audioId).map(_ => None)
          else
            validateAndUpdateMetaData(audioId, newAudio, existing, None).map(Some(_))

        case Some(_) =>
          Failure(new NotFoundException(s"Audio with id $audioId does not exist in language '$language'."))
        case None =>
          Failure(new NotFoundException(s"Audio with id $audioId was not found, and could not be deleted."))
      }

    def storeNewAudio(newAudioMeta: NewAudioMetaInformation, file: FileItem): Try[AudioMetaInformation] = {
      validationService.validateAudioFile(file) match {
        case Some(validationMessage) => Failure(new ValidationException(errors = Seq(validationMessage)))
        case None =>
          val audioFileMeta = uploadFile(file, newAudioMeta.language) match {
            case Failure(e)         => return Failure(e)
            case Success(audioMeta) => audioMeta
          }

          val audioMetaInformation = for {
            domainAudio <- Try(converterService.toDomainAudioMetaInformation(newAudioMeta, audioFileMeta))
            _ <- validationService.validate(domainAudio, None)
            audioMetaData <- Try(audioRepository.insert(domainAudio))
            _ <- searchIndexService.indexDocument(audioMetaData)
          } yield converterService.toApiAudioMetaInformation(audioMetaData, Some(newAudioMeta.language))

          if (audioMetaInformation.isFailure) {
            deleteFile(audioFileMeta)
          }

          audioMetaInformation.flatten
      }
    }

    def deleteAudioAndFiles(audioId: Long) = {
      audioRepository
        .withId(audioId) match {
        case Some(toDelete) =>
          val metaDeleted = audioRepository.deleteAudio(audioId)
          val filesDeleted = toDelete.filePaths.map(fileToDelete => {
            deleteFile(fileToDelete) match {
              case Failure(ex) =>
                Failure(
                  new AudioStorageException(
                    s"Deletion of file at '${fileToDelete.filePath}' failed with: ${ex.getMessage}"))
              case ok => ok
            }
          })
          val indexDeleted = searchIndexService.deleteDocument(audioId)

          if (metaDeleted < 1) {
            Failure(
              new NotFoundException(s"Metadata for audio with id $audioId was not found, and could not be deleted."))
          } else if (filesDeleted.exists(_.isFailure)) {
            val exceptions = filesDeleted.collect { case Failure(ex) => ex }
            val msg = exceptions.map(_.getMessage).mkString("\n")
            Failure(new AudioStorageException(msg))
          } else {
            indexDeleted match {
              case Failure(ex)   => Failure(ex)
              case Success(true) => Success(audioId)
              case Success(false) =>
                Failure(ElasticIndexingException(s"Something went wrong when deleting search index of $audioId"))
            }
          }

        case None => Failure(new NotFoundException(s"Audio with id $audioId was not found, and could not be deleted."))
      }
    }

    def updateAudio(id: Long,
                    metadataToUpdate: UpdatedAudioMetaInformation,
                    fileOpt: Option[FileItem]): Try[AudioMetaInformation] = {
      audioRepository.withId(id) match {
        case None => Failure(new NotFoundException)
        case Some(existingMetadata) => {
          val metadataAndFile = fileOpt match {
            case None => Success(mergeAudioMeta(existingMetadata, metadataToUpdate))
            case Some(file) => {
              val validationMessages = validationService.validateAudioFile(file)
              if (validationMessages.nonEmpty) {
                return Failure(new ValidationException(errors = Seq(validationMessages.get)))
              }

              uploadFile(file, metadataToUpdate.language) match {
                case Failure(err) => Failure(err)
                case Success(uploadedFile) =>
                  Success(mergeAudioMeta(existingMetadata, metadataToUpdate, Some(uploadedFile)))
              }
            }
          }

          val savedAudio = metadataAndFile.map(_._2)
          val metadataToSave = metadataAndFile.map(_._1)

          val finished =
            metadataToSave.flatMap(validateAndUpdateMetaData(id, _, existingMetadata, Some(metadataToUpdate.language)))

          if (finished.isFailure && !savedAudio.isFailure) {
            savedAudio.get.foreach(deleteFile)
          }

          finished
        }
      }
    }

    private def validateAndUpdateMetaData(audioId: Long,
                                          toSave: domain.AudioMetaInformation,
                                          oldAudio: domain.AudioMetaInformation,
                                          language: Option[String]) = {
      for {
        validated <- validationService.validate(toSave, Some(oldAudio))
        updated <- audioRepository.update(validated, audioId)
        indexed <- searchIndexService.indexDocument(updated)
        converted <- converterService.toApiAudioMetaInformation(indexed, language)
      } yield converted
    }

    private[service] def mergeAudioMeta(
        existing: domain.AudioMetaInformation,
        toUpdate: UpdatedAudioMetaInformation,
        savedAudio: Option[Audio] = None): (domain.AudioMetaInformation, Option[Audio]) = {
      val mergedFilePaths = savedAudio match {
        case None => existing.filePaths
        case Some(audio) =>
          mergeLanguageField[Audio, domain.Audio](
            existing.filePaths,
            domain.Audio(audio.filePath, audio.mimeType, audio.fileSize, audio.language))
      }

      val merged = existing.copy(
        revision = Some(toUpdate.revision),
        titles =
          mergeLanguageField[String, domain.Title](existing.titles, domain.Title(toUpdate.title, toUpdate.language)),
        tags = mergeLanguageField[Seq[String], domain.Tag](existing.tags, domain.Tag(toUpdate.tags, toUpdate.language)),
        filePaths = mergedFilePaths,
        copyright = converterService.toDomainCopyright(toUpdate.copyright),
        updated = clock.now(),
        updatedBy = authUser.userOrClientid()
      )
      (merged, savedAudio)
    }

    private[service] def mergeLanguageField[T, Y <: LanguageField[T]](field: Seq[Y], toMerge: Y): Seq[Y] = {
      field.indexWhere(_.language == toMerge.language) match {
        case idx if idx >= 0 => field.patch(idx, Seq(toMerge), 1)
        case _               => field ++ Seq(toMerge)
      }
    }

    private[service] def deleteFile(audioFile: Audio) = {
      audioStorage.deleteObject(audioFile.filePath)
    }

    private[service] def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.substring(index))
        case _                        => None
      }
    }

    private[service] def uploadFile(file: FileItem, language: String): Try[Audio] = {
      val fileExtension = getFileExtension(file.name).getOrElse("")
      val contentType = file.getContentType.getOrElse("")
      val fileName = LazyList.continually(randomFileName(fileExtension)).dropWhile(audioStorage.objectExists).head

      audioStorage
        .storeAudio(new ByteArrayInputStream(file.get), contentType, file.size, fileName)
        .map(objectMeta => Audio(fileName, objectMeta.getContentType, objectMeta.getContentLength, language))
    }

    private[service] def randomFileName(extension: String, length: Int = 12): String = {
      val extensionWithDot = if (extension.head == '.') extension else s".$extension"
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

  }
}
