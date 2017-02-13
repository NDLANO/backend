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
    def storeNewAudio(newAudioMeta: NewAudioMetaInformation, files: Seq[FileItem]): Try[AudioMetaInformation] = {
      val fileValidationMessages = files.flatMap(validationService.validateAudioFile)
      if (fileValidationMessages.nonEmpty) {
        return Failure(new ValidationException(errors=fileValidationMessages))
      }

      for {
        uploadedAudios <- uploadFiles(files, newAudioMeta.audioFiles)
        domainAudio <-  Try(converterService.toDomainAudioMetaInformation(newAudioMeta, uploadedAudios))
        _ <- validationService.validate(domainAudio)
        r <- Try(audioRepository.insert(domainAudio))
        _ <- searchIndexService.indexDocument(r)
      } yield converterService.toApiAudioMetaInformation(r)
    }

    private[service] def uploadFiles(filesToUpload: Seq[FileItem], audioFileMetas: Seq[NewAudioFile]): Try[Seq[Audio]] = {
      val uploadedFiles = filesToUpload flatMap(x => uploadFile(x).toOption) toMap

      if (uploadedFiles.size != filesToUpload.size) {
        deleteFiles(uploadedFiles.values.toSeq)
        return Failure(new RuntimeException("Failed to save file(s)"))
      }

      val uploaded = uploadedFiles map { case (oldFileName, uploadedFile) =>
        getLanguageForFile(oldFileName, audioFileMetas).map(language => uploadedFile.copy(language = language))
      }

      val failedToFindLanguage: Seq[String] = uploaded.filter(_.isFailure).map(_.failed.get.getMessage).toSeq
      failedToFindLanguage.headOption match {
        case Some(_) =>
          deleteFiles(uploadedFiles.values.toSeq)
          Failure(new ValidationException(errors = Seq(ValidationMessage("audioFiles", failedToFindLanguage.mkString(",")))))
        case _ => Success(uploaded.flatMap(_.toOption).toSeq)
      }
    }

    private[service] def deleteFiles(audioFiles: Seq[Audio]) = {
      audioFiles.foreach(fileToDelete => audioStorage.deleteObject(fileToDelete.filePath))
    }

    private[service] def getLanguageForFile(oldFileName: String, audioFileMetas: Seq[NewAudioFile]): Try[Option[String]] = {
      audioFileMetas.find(_.fileName == oldFileName) match {
        case Some(e) => Success(e.language)
        case None => Failure(new RuntimeException(s"Could not find entry for file '$oldFileName' in metadata"))
      }
    }

    private[service] def uploadFile(file: FileItem): Try[(String, Audio)] = {
      val contentType = file.getContentType.getOrElse("")
      val fileName = Stream.continually(randomFileName(".mp3")).dropWhile(audioStorage.objectExists).head

      audioStorage.storeAudio(new ByteArrayInputStream(file.get), contentType, file.size, fileName).map(filePath => {
        file.name -> Audio(filePath, contentType, file.size, None)
      })
    }

    private[service] def randomFileName(extension: String, length: Int = 12): String = {
      val extensionWithDot = if (extension.head == '.') extension else s".$extension"
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

  }
}
