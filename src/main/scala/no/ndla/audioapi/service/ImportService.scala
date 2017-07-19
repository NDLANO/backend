/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.auth.User
import no.ndla.audioapi.integration.{MigrationApiClient, MigrationAudioMeta}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.repository.AudioRepository

import scala.util.{Success, Try}

trait ImportService {
  this: MigrationApiClient with AudioStorageService with AudioRepository with TagsService with User with Clock =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    def importAudio(audioId: String): Try[domain.AudioMetaInformation] = {
      migrationApiClient.getAudioMetaData(audioId).map(uploadAndPersist)
    }

    private def uploadAndPersist(audioMeta: Seq[MigrationAudioMeta]): domain.AudioMetaInformation = {
      val audioFilePaths = audioMeta.map(uploadAudioFile(_).get)
      persistMetaData(audioMeta, audioFilePaths)
    }

    private def cleanAudioMeta(audio: domain.AudioMetaInformation): domain.AudioMetaInformation = {
      val titleLanguages = audio.titles.map(_.language)
      val tags = audio.tags.filter(tag => titleLanguages.contains(tag.language))

      audio.copy(tags = tags)
    }

    private def persistMetaData(audioMeta: Seq[MigrationAudioMeta], audioObjects: Seq[Audio]): domain.AudioMetaInformation = {
      val titles = audioMeta.map(x => Title(x.title, x.language))
      val mainNode = audioMeta.find(_.isMainNode).get
      val authors = audioMeta.flatMap(_.authors).distinct
      val origin = authors.find(_.`type`.toLowerCase() == "opphavsmann")
      val copyright = Copyright(mainNode.license, origin.map(_.name), authors.diff(Seq(origin)).map(x => Author(x.`type`, x.name)))
      val domainMetaData = cleanAudioMeta(domain.AudioMetaInformation(None, titles, audioObjects, copyright, tagsService.forAudio(mainNode.nid), "content-import-client", clock.now()))

      audioRepository.withExternalId(mainNode.nid) match {
        case None => audioRepository.insertFromImport(domainMetaData, mainNode.nid)
        case Some(existingAudio) => audioRepository.update(domainMetaData, existingAudio.id.get)
      }
    }

    private def uploadAudioFile(audioMeta: MigrationAudioMeta): Try[Audio] = {
      val fileLocationTry = audioStorage.objectExists(audioMeta.fileName) match {
        case true => Success(audioMeta.fileName)
        case false => audioStorage.storeAudio(new URL(audioMeta.url), audioMeta.mimeType, audioMeta.fileSize, audioMeta.fileName)
      }

      fileLocationTry.map(fileLocation => Audio(fileLocation, audioMeta.mimeType, audioMeta.fileSize.toLong, audioMeta.language))
    }

  }
}
