/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import java.net.URL

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.model.ObjectMetadata
import no.ndla.audioapi.integration.MigrationAudioMeta
import no.ndla.audioapi.integration.MigrationAuthor
import no.ndla.audioapi.model.domain.{AudioMetaInformation, Author}
import no.ndla.audioapi.model.api.ImportException
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.network.model.HttpRequestException
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Failure, Success}

class ImportServiceTest extends UnitSuite with TestEnvironment {
  val service = new ImportService

  val s3ObjectMock = mock[ObjectMetadata]
  val audioId = "1234"

  val defaultMigrationAudioMeta = MigrationAudioMeta("1",
                                                     "1",
                                                     "title",
                                                     "file.mp3",
                                                     "http://something.com",
                                                     "audio/mpeg",
                                                     "1024",
                                                     None,
                                                     "by-sa",
                                                     Seq())

  override def beforeEach() = {
    when(s3ObjectMock.getContentLength).thenReturn(1024)
    when(s3ObjectMock.getContentType).thenReturn("audio/mp3")
  }

  test("importAudio returns Failure if call to migration api failes") {
    val audioId = "1234"
    val exceptionMock = mock[HttpRequestException]
    when(migrationApiClient.getAudioMetaData(audioId)).thenReturn(Failure(exceptionMock))
    service.importAudio(audioId) should equal(Failure(exceptionMock))
  }

  test("importAudio returns Failure if upload to S3 fails") {
    val clientException = mock[AmazonClientException]

    when(migrationApiClient.getAudioMetaData(audioId)).thenReturn(Success(Seq(defaultMigrationAudioMeta)))

    when(audioStorage.getObjectMetaData(any[String])).thenReturn(Failure(mock[AmazonClientException]))
    when(audioStorage.storeAudio(any[URL], any[String], any[String], any[String])).thenReturn(Failure(clientException))
    service.importAudio(audioId) should equal(Failure(clientException))
  }

  test("importAudio updates the database entry if already exists") {
    val audioPath = "audio/file.mp3"
    val existingAudioMeta = mock[AudioMetaInformation]

    when(migrationApiClient.getAudioMetaData(audioId)).thenReturn(Success(Seq(defaultMigrationAudioMeta)))
    when(audioStorage.getObjectMetaData(any[String])).thenReturn(Failure(mock[AmazonClientException]))
    when(audioStorage.storeAudio(any[URL], any[String], any[String], any[String])).thenReturn(Success(s3ObjectMock))
    when(tagsService.forAudio("1")).thenReturn(List())
    when(audioRepository.withExternalId(defaultMigrationAudioMeta.nid)).thenReturn(Some(existingAudioMeta))
    when(existingAudioMeta.id).thenReturn(Some(1: Long))
    when(audioRepository.update(any[AudioMetaInformation], any[Long])).thenReturn(Success(existingAudioMeta))

    service.importAudio(audioId) should equal(Success(existingAudioMeta))
    verify(audioRepository, times(1)).update(any[AudioMetaInformation], any[Long])
  }

  test("importAudio inserts a new database entry if already exists") {
    val audioPath = "audio/file.mp3"
    val newAudioMeta = mock[AudioMetaInformation]

    when(migrationApiClient.getAudioMetaData(audioId)).thenReturn(Success(Seq(defaultMigrationAudioMeta)))
    when(audioStorage.storeAudio(any[URL], any[String], any[String], any[String])).thenReturn(Success(s3ObjectMock))
    when(tagsService.forAudio("1")).thenReturn(List())
    when(audioRepository.withExternalId(defaultMigrationAudioMeta.nid)).thenReturn(None)
    when(audioRepository.insertFromImport(any[AudioMetaInformation], any[String])).thenReturn(Success(newAudioMeta))

    service.importAudio(audioId) should equal(Success(newAudioMeta))
    verify(audioRepository, times(1)).insertFromImport(any[AudioMetaInformation], any[String])
  }

  test("That authors are translated correctly") {
    val authors = List(
      MigrationAuthor("Opphavsmann", "A"),
      MigrationAuthor("Redaksjonelt", "B"),
      MigrationAuthor("redaKsJoNelT", "C"),
      MigrationAuthor("distributør", "D"),
      MigrationAuthor("leVerandør", "E"),
      MigrationAuthor("Språklig", "F")
    )
    val meta =
      MigrationAudioMeta("1", "1", "Lydar", "lydar.mp3", "lydary", "file/mp3", "123141", Some("nb"), "by-sa", authors)

    val copyright = service.toDomainCopyright("by-sa", authors)
    copyright.creators should contain(Author("Originator", "A"))

    copyright.rightsholders should contain(Author("Distributor", "D"))
    copyright.rightsholders should contain(Author("Supplier", "E"))

    copyright.processors should contain(Author("Linguistic", "F"))
    copyright.processors should contain(Author("Editorial", "B"))
    copyright.processors should contain(Author("Editorial", "C"))

  }

  test("That oldToNewLicenseKey throws on invalid license") {
    assertThrows[ImportException] {
      service.oldToNewLicenseKey("publicdomain")
    }
  }

  test("That oldToNewLicenseKey converts correctly") {
    service.oldToNewLicenseKey("nolaw") should be("cc0")
    service.oldToNewLicenseKey("noc") should be("pd")
  }

  test("That oldToNewLicenseKey does not convert an license that should not be converted") {
    service.oldToNewLicenseKey("by-sa") should be("by-sa")
  }
}
