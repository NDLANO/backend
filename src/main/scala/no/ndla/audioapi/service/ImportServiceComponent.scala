/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.{MigrationApiClient, MigrationAudioMeta}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.repository.AudioRepositoryComponent

import scala.util.Try

trait ImportServiceComponent {
  this: MigrationApiClient with AudioStorageService with AudioRepositoryComponent =>
  val importService: ImportService

  class ImportService {
    def importAudio(audioId: String): Try[domain.AudioMetaInformation] = {
      migrationApiClient.getAudioMetaData(audioId).flatMap(uploadAndPersist)
    }

    def uploadAndPersist(audioMeta: MigrationAudioMeta): Try[domain.AudioMetaInformation] =
      uploadAudioFile(audioMeta).map(audioLocation => persistMetaData(audioMeta, audioLocation))

    def persistMetaData(audioMeta: MigrationAudioMeta, audioLocation: String): domain.AudioMetaInformation = {
      val domainMetaData = domain.AudioMetaInformation(None, audioMeta.title, audioLocation, audioMeta.mimeType, audioMeta.fileSize.toLong)
      audioRepository.withExternalId(audioMeta.nid) match {
        case None => audioRepository.insert(domainMetaData, audioMeta.nid)
        case Some(existingAudio) => audioRepository.update(domainMetaData, existingAudio.id.get)
      }
    }

    def uploadAudioFile(audioMeta: MigrationAudioMeta): Try[String] =
      audioStorage.storeAudio(audioMeta.url, s"${AudioApiProperties.AudioUrlContextPath}/${audioMeta.fileName}")
  }
}
