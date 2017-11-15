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
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.model.{Language, domain}
import no.ndla.audioapi.repository.AudioRepository

import scala.util.Try
import com.netaporter.uri.dsl._

trait ImportService {
  this: MigrationApiClient with AudioStorageService with AudioRepository with TagsService with User with Clock =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    def importAudio(audioId: String): Try[domain.AudioMetaInformation] = {
      migrationApiClient.getAudioMetaData(audioId).flatMap(uploadAndPersist)
    }

    private def uploadAndPersist(audioMeta: Seq[MigrationAudioMeta]): Try[domain.AudioMetaInformation] = {
      val audioFilePaths = audioMeta.map(uploadAudioFile(_).get)
      persistMetaData(audioMeta, audioFilePaths)
    }

    private def cleanAudioMeta(audio: domain.AudioMetaInformation): domain.AudioMetaInformation = {
      val titleLanguages = audio.titles.map(_.language)
      val tags = audio.tags.filter(tag => titleLanguages.contains(tag.language))

      audio.copy(tags = tags)
    }

    private def persistMetaData(audioMeta: Seq[MigrationAudioMeta], audioObjects: Seq[Audio]): Try[domain.AudioMetaInformation] = {
      val titles = audioMeta.map(x => Title(x.title, Language.languageOrUnknown(x.language)))
      val mainNode = audioMeta.find(_.isMainNode).get
      val authors = audioMeta.flatMap(_.authors).distinct
      val origin = authors.find(_.`type`.toLowerCase() == "opphavsmann")
      val copyright = Copyright(mainNode.license, origin.map(_.name), authors.diff(Seq(origin)).map(x => Author(x.`type`, x.name)))
      val domainMetaData = cleanAudioMeta(domain.AudioMetaInformation(None, None, titles, audioObjects, copyright, tagsService.forAudio(mainNode.nid), authUser.userOrClientid(), clock.now()))

      audioRepository.withExternalId(mainNode.nid) match {
        case None => audioRepository.insertFromImport(domainMetaData, mainNode.nid)
        case Some(existingAudio) => audioRepository.update(domainMetaData.copy(revision = existingAudio.revision), existingAudio.id.get)
      }
    }

    private def uploadAudioFile(audioMeta: MigrationAudioMeta): Try[Audio] = {
      audioStorage.getObjectMetaData(audioMeta.fileName)
        .orElse(audioStorage.storeAudio(new URL(audioMeta.url.withScheme("https")), audioMeta.mimeType, audioMeta.fileSize, audioMeta.fileName))
        .map(s3ObjectMeta => Audio(audioMeta.fileName, s3ObjectMeta.getContentType, s3ObjectMeta.getContentLength, Language.languageOrUnknown(audioMeta.language)))
    }

  }
}
