package no.ndla.audioapi.service

import java.io.InputStream

import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain.Audio
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatra.servlet.FileItem
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val writeService = new WriteService
  override val converterService = new ConverterService
  val (newFileName1, newFileName2) = ("AbCdeF.mp3", "GhijKl.mp3")
  val newAudioFile1 = NewAudioFile("test.mp3", Some("nb"))
  val newAudioFile2 = NewAudioFile("test2.mp3", Some("nb"))
  val fileMock1: FileItem = mock[FileItem]
  val fileMock2: FileItem = mock[FileItem]

  val newAudioMeta = NewAudioMetaInformation(
    Seq(Title("title", Some("en"))),
    Seq(newAudioFile1),
    Copyright(License("by", None, None), None, Seq()),
    Option(Seq(Tag(Seq("tag"), Some("en"))))
  )
  val domainAudioMeta = converterService.toDomainAudioMetaInformation(newAudioMeta, Seq(Audio(newFileName1, "audio/mp3", 1024, Some("en"))))
  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate


  override def beforeEach = {
    when(fileMock1.getContentType).thenReturn(Some("audio/mp3"))
    when(fileMock1.get).thenReturn(Array[Byte](0x49, 0x44, 0x33))
    when(fileMock1.size).thenReturn(1024)
    when(fileMock1.name).thenReturn(newAudioFile1.fileName)

    when(fileMock2.getContentType).thenReturn(Some("audio/mp3"))
    when(fileMock2.get).thenReturn(Array[Byte](0x49, 0x44, 0x33))
    when(fileMock2.size).thenReturn(2048)
    when(fileMock2.name).thenReturn(newAudioFile2.fileName)

    reset(audioRepository, searchIndexService)
    when(audioRepository.insert(any[domain.AudioMetaInformation])(any[DBSession])).thenReturn(domainAudioMeta.copy(id=Some(1)))
  }

  test("converter to domain should set updatedBy from authUser and updated date"){
    when(authUser.id()).thenReturn("ndla54321")
    when(clock.now()).thenReturn(updated())
    val domain = converterService.toDomainAudioMetaInformation(newAudioMeta, Seq(Audio(newFileName1, "audio/mp3", 1024, Some("en"))))
    domain.updatedBy should equal ("ndla54321")
    domain.updated should equal(updated())
  }

  test("randomFileName should return a random filename with a given length and extension") {
    val extension = ".mp3"

    val result = writeService.randomFileName(extension)
    result.length should be (12)
    result.endsWith(extension) should be (true)

    val resultWithNegativeLength = writeService.randomFileName(extension, -1)
    resultWithNegativeLength.length should be (1 + extension.length)
    resultWithNegativeLength.endsWith(extension) should be (true)
  }

  test("uploadFile should return Success if file upload succeeds") {
    when(audioStorage.objectExists(any[String])).thenReturn(false)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))
    val result = writeService.uploadFile(fileMock1)
    verify(audioStorage, times(1)).storeAudio(any[InputStream], any[String], any[Long], any[String])

    result should equal(Success(newAudioFile1.fileName, Audio(newFileName1, "audio/mp3", 1024, None)))
  }

  test("uploadFiles should return Failure if file upload failed") {
    when(audioStorage.objectExists(any[String])).thenReturn(false)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException))

    writeService.uploadFile(fileMock1).isFailure should be (true)
  }

  test("getLanguageForFile should return Success if metadata contains entry with name of file to be uploaded") {
    val newFiles = Seq(newAudioFile1, newAudioFile2)
    writeService.getLanguageForFile(newAudioFile1.fileName, newFiles) should equal(Success(Some("nb")))
  }

  test("getLanguageForFile should return Failure if metadata does not contain entry with name of file to be uploaded") {
    writeService.getLanguageForFile("random.mp3", Seq(newAudioFile1)).isFailure should be (true)
  }

  test("deleteFiles should delete all files in a list") {
    writeService.deleteFiles(Seq(Audio("mp3.mp3", "audio/mp3", 1024, None), Audio("hello.mp3", "audio/mp3", 1024, Some("en"))))
    verify(audioStorage, times(2)).deleteObject(any[String])
  }

  test("uploadFiles should return Failure if some files fails to upload") {
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException))

    val result = writeService.uploadFiles(Seq(fileMock1, fileMock2), Seq(newAudioFile1, newAudioFile2))

    result.isFailure should be(true)
    result.failed.get.getMessage should equal ("Failed to save file(s)")
  }

  test("uploadFiles should return Failure if entry for does not exist in metadata") {
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))
    val result = writeService.uploadFiles(Seq(fileMock1), Seq(newAudioFile2))

    result.isFailure should be (true)
    val errors: Seq[ValidationMessage] = result.failed.get.asInstanceOf[ValidationException].errors
    errors.length should be (1)
    errors.head.message.contains("Could not find entry for file") should be (true)
    verify(audioStorage, times(1)).deleteObject(newFileName1)
  }

  test("uploadFiles should return a sequence of Audio objects if everything went ok") {
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))
    val result = writeService.uploadFiles(Seq(fileMock1), Seq(newAudioFile1))

    result.isSuccess should be (true)
    result should equal (Success(Seq(Audio(newFileName1, "audio/mp3", 1024, newAudioFile1.language))))
  }

  test("storeNewAudio should return Failure if filetype is invalid") {
    when(fileMock1.contentType).thenReturn(Some("application/text"))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)

    writeService.storeNewAudio(newAudioMeta, Seq(fileMock1)).isFailure should be (true)
  }

  test("storeNewAudio should return Failure if upload failes") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewAudio(newAudioMeta, Seq(fileMock1)).isFailure should be (true)
  }

  test("storeNewAudio should return Failure if validation fails") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Failure(new ValidationException(errors=Seq())))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))

    writeService.storeNewAudio(newAudioMeta, Seq(fileMock1)).isFailure should be (true)
    verify(audioRepository, times(0)).insert(any[domain.AudioMetaInformation])(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("storeNewAudio should return Failure if failed to insert into database") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))
    when(audioRepository.insert(any[domain.AudioMetaInformation])(any[DBSession])).thenThrow(new RuntimeException)

    writeService.storeNewAudio(newAudioMeta, Seq(fileMock1)).isFailure should be (true)
    verify(searchIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("storeNewAudio should return Failure if failed to index audio metadata") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))
    when(searchIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewAudio(newAudioMeta, Seq(fileMock1)).isFailure should be (true)
    verify(audioRepository, times(1)).insert(any[domain.AudioMetaInformation])(any[DBSession])
  }

  test("storeNewAudio should return Success if creation of new audio file succeeded") {
    val afterInsert = domainAudioMeta.copy(id=Some(1))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(newFileName1))
    when(searchIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))

    val result = writeService.storeNewAudio(newAudioMeta, Seq(fileMock1))
    result.isSuccess should be (true)
    result should equal(converterService.toApiAudioMetaInformation(afterInsert, afterInsert.tags.head.language.get))

    verify(audioRepository, times(1)).insert(any[domain.AudioMetaInformation])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.AudioMetaInformation])
  }
}
