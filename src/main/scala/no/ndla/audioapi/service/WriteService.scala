package no.ndla.audioapi.service

import no.ndla.audioapi.model.api.{AudioMetaInformation, NewAudioMetaInformation}
import no.ndla.audioapi.model.domain.Audio
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.SearchIndexService

import scala.util.Try

trait WriteService {
  this: ConverterService with ValidationService with AudioRepository with SearchIndexService =>
  val writeService: WriteService

  class WriteService {
    def storeNewAudio(resource: NewAudioMetaInformation): Try[AudioMetaInformation] = {
      val audioPaths = Seq[Audio]() // TODO: (decode b64 string)?, validate, generate filename, upload to s3)
      val domainAudio = converterService.toDomainAudioMetaInformation(resource, audioPaths)

      for {
        _ <- validationService.validate(domainAudio)
        r <- Try(audioRepository.insert(domainAudio))
        _ <- searchIndexService.indexDocument(r)
      } yield converterService.toApiAudioMetaInformation(r)
    }

  }
}
