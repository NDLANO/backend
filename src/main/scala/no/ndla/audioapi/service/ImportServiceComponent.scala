/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.{MigrationApiClient, MigrationAudioMeta}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain.{AudioFilePath, AudioInfo, AudioTitle}
import no.ndla.audioapi.repository.AudioRepositoryComponent

import scala.util.{Success, Try}

trait ImportServiceComponent {
  this: MigrationApiClient with AudioStorageService with AudioRepositoryComponent =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    def importAudio(audioId: String): Try[domain.AudioMetaInformation] = {
      logger.warn("Importing node with id {}", audioId)
      migrationApiClient.getAudioMetaData(audioId).map(uploadAndPersist)
    }

    private def uploadAndPersist(audioMeta: Seq[MigrationAudioMeta]): domain.AudioMetaInformation = {
      logger.warn("upload and persist")

      val audioFilePaths = audioMeta.map(uploadAudioFile(_).get)
      persistMetaData(audioMeta, audioFilePaths)
    }

    private def persistMetaData(audioMeta: Seq[MigrationAudioMeta], audioFilePaths: Seq[AudioFilePath]): domain.AudioMetaInformation = {
      logger.warn("persist")

      val (titles, infos) = audioMeta.map(x => (AudioTitle(x.title, x.language),
                                                AudioInfo(x.mimeType, x.fileSize.toLong, x.language))).unzip
      val mainNodeId = audioMeta.find(_.isMainNode).get.nid
      val domainMetaData = domain.AudioMetaInformation(None, titles, audioFilePaths, infos)

      audioRepository.withExternalId(mainNodeId) match {
        case None => audioRepository.insert(domainMetaData, mainNodeId)
        case Some(existingAudio) => audioRepository.update(domainMetaData, existingAudio.id.get)
      }
    }

    private def uploadAudioFile(audioMeta: MigrationAudioMeta): Try[AudioFilePath] = {
      logger.warn("upload")

      val destinationPath = s"${AudioApiProperties.AudioUrlContextPath}/${audioMeta.fileName}"
      audioStorage.storeAudio(audioMeta.url, destinationPath).map(AudioFilePath(_, audioMeta.language))
    }
  }
}
