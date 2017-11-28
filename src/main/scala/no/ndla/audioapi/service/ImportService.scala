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
import no.ndla.audioapi.integration.{MigrationApiClient, MigrationAudioMeta, MigrationAuthor}
import no.ndla.audioapi.model.api.ImportException
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.model.{Language, domain}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.AudioApiProperties._
import no.ndla.mapping.License._

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

    private def toNewAuthorType(author: MigrationAuthor): domain.Author = {
      val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
      val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
      val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

      (creatorMap(author.`type`.toLowerCase), processorMap(author.`type`.toLowerCase), rightsholderMap(author.`type`.toLowerCase)) match {
        case (t: String, None, None) => domain.Author(t.capitalize, author.name)
        case (None, t: String, None) => domain.Author(t.capitalize, author.name)
        case (None, None, t: String) => domain.Author(t.capitalize, author.name)
        case (_, _, _) => domain.Author(author.`type`, author.name)
      }
    }

    private def mapOldToNewLicenseKey(license: String): String = {
      val licenses = Map("nolaw" -> "cc0", "noc" -> "pd")
      val newLicense = licenses.getOrElse(license, license)

      if (getLicense(newLicense).isEmpty) {
        throw new ImportException(s"License $license is not supported.")
      }
      newLicense
    }

    private def persistMetaData(audioMeta: Seq[MigrationAudioMeta], audioObjects: Seq[Audio]): Try[domain.AudioMetaInformation] = {
      val titles = audioMeta.map(x => Title(x.title, Language.languageOrUnknown(x.language)))
      val mainNode = audioMeta.find(_.isMainNode).get
      val authors = audioMeta.flatMap(_.authors).distinct
      val origin = authors.find(_.`type`.toLowerCase() == "opphavsmann")

      val creators = authors.filter(a => oldCreatorTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)
      // Filters out processor authors with old type `redaksjonelt` during import process since `redaksjonelt` exists both in processors and creators.
      val processors = authors.filter(a => oldProcessorTypes.contains(a.`type`.toLowerCase)).filterNot(a => a.`type`.toLowerCase == "redaksjonelt").map(toNewAuthorType)
      val rightsholders = authors.filter(a => oldRightsholderTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)

      val copyright = domain.Copyright(
        mapOldToNewLicenseKey(mainNode.license),
        origin.map(_.name),
        creators,
        processors,
        rightsholders,
        None,
        None,
        None
      )
      val domainMetaData = cleanAudioMeta(domain.AudioMetaInformation(None, None, titles, audioObjects, copyright, tagsService.forAudio(mainNode.nid), "content-import-client", clock.now()))

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
