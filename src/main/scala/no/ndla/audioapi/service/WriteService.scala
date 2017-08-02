package no.ndla.audioapi.service

import java.io.ByteArrayInputStream

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.Audio
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.SearchIndexService
import org.scalatra.servlet.FileItem
import scala.util.{Failure, Random, Success, Try}
import java.lang.Math.max


trait WriteService {
  this: ConverterService with ValidationService with AudioRepository with SearchIndexService with AudioStorageService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {
    def storeNewAudio(newAudioMeta: NewAudioMetaInformation, file: FileItem): Try[AudioMetaInformation] = {
      val fileValidationMessages = validationService.validateAudioFile(file)
      if (fileValidationMessages.nonEmpty) {
        return Failure(new ValidationException(errors=Seq(fileValidationMessages.get)))
      }

      val audioFileMeta = uploadFile(file, newAudioMeta.language) match {
        case Failure(e) => return Failure(e)
        case Success(audioMeta) => audioMeta
      }

      val audioMetaInformation = for {
        domainAudio <-  Try(converterService.toDomainAudioMetaInformation(newAudioMeta, audioFileMeta))
        _ <- validationService.validate(domainAudio)
        audioMetaData <- Try(audioRepository.insert(domainAudio))
        _ <- searchIndexService.indexDocument(audioMetaData)
      } yield converterService.toApiAudioMetaInformation(audioMetaData, audioMetaData.titles.head.language.get)

      if (audioMetaInformation.isFailure) {
        deleteFile(audioFileMeta)
      }

      audioMetaInformation.flatten
    }

    private[service] def deleteFile(audioFile: Audio) = {
      audioStorage.deleteObject(audioFile.filePath)
    }

    private[service] def getLanguageForFile(oldFileName: String, audioFileMetas: Seq[NewAudioFile]): Try[Option[String]] = {
      audioFileMetas.find(_.fileName == oldFileName) match {
        case Some(e) => Success(e.language)
        case None => Failure(new LanguageMappingException(s"Could not find entry for file '$oldFileName' in metadata"))
      }
    }

    private[service] def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.substring(index))
        case _ => None
      }
    }

    private[service] def uploadFile(file: FileItem, language: String): Try[Audio] = {
      val fileExtension = getFileExtension(file.name).getOrElse("")
      val contentType = file.getContentType.getOrElse("")
      val fileName = Stream.continually(randomFileName(fileExtension)).dropWhile(audioStorage.objectExists).head

      audioStorage.storeAudio(new ByteArrayInputStream(file.get), contentType, file.size, fileName).map(filePath => {
        Audio(filePath, contentType, file.size, Some(language))
      })
    }

    private[service] def randomFileName(extension: String, length: Int = 12): String = {
      val extensionWithDot = if (extension.head == '.') extension else s".$extension"
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

  }
}
