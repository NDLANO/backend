package no.ndla.audioapi.service

import java.io.InputStream

import com.amazonaws.services.s3.model.ObjectMetadata
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
  val newAudioFile1 = NewAudioFile("test.mp3", "nb")
  val newAudioFile2 = NewAudioFile("test2.mp3", "nb")
  val fileMock1: FileItem = mock[FileItem]
  val fileMock2: FileItem = mock[FileItem]
  val s3ObjectMock = mock[ObjectMetadata]

  val newAudioMeta = NewAudioMetaInformation(
    "title",
    "en",
    Copyright(License("by", None, None), None, Seq()),
    Seq("tag"))

  val updatedAudioMeta = UpdatedAudioMetaInformation(
    revision = 1,
    title = "title",
    language = "en",
    copyright = Copyright(License("by", None, None), None, Seq()),
    tags = Seq("tag"))

  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  
  val someAudio = Audio(newFileName1, "audio/mp3", 1024, "en")

  val domainAudioMeta = converterService.toDomainAudioMetaInformation(newAudioMeta, someAudio)


  override def beforeEach = {
    when(fileMock1.getContentType).thenReturn(Some("audio/mp3"))
    when(fileMock1.get).thenReturn(Array[Byte](0x49, 0x44, 0x33))
    when(fileMock1.size).thenReturn(1024)
    when(fileMock1.name).thenReturn(newAudioFile1.fileName)

    when(fileMock2.getContentType).thenReturn(Some("audio/mp3"))
    when(fileMock2.get).thenReturn(Array[Byte](0x49, 0x44, 0x33))
    when(fileMock2.size).thenReturn(2048)
    when(fileMock2.name).thenReturn(newAudioFile2.fileName)

    when(s3ObjectMock.getContentLength).thenReturn(1024)
    when(s3ObjectMock.getContentType).thenReturn("audio/mp3")

    reset(audioRepository, searchIndexService, audioStorage)
    when(audioRepository.insert(any[domain.AudioMetaInformation])(any[DBSession])).thenReturn(domainAudioMeta.copy(id=Some(1), revision=Some(1)))
  }

  test("converter to domain should set updatedBy from authUser and updated date"){
    when(authUser.id()).thenReturn("ndla54321")
    when(clock.now()).thenReturn(updated)
    val domain = converterService.toDomainAudioMetaInformation(newAudioMeta, Audio(newFileName1, "audio/mp3", 1024, "en"))
    domain.updatedBy should equal ("ndla54321")
    domain.updated should equal(updated)
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

  test("uploadFiles should return Failure if file upload failed") {
    when(audioStorage.objectExists(any[String])).thenReturn(false)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException))

    writeService.uploadFile(fileMock1, "en").isFailure should be (true)
  }

  test("deleteFiles should delete all files in a list") {
    writeService.deleteFile(Audio("mp3.mp3", "audio/mp3", 1024, "unknown"))
    verify(audioStorage, times(1)).deleteObject(any[String])
  }

  test("uploadFile should return Failure if storeFile fails to upload") {
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException("Failed to save file")))

    val result = writeService.uploadFile(fileMock1, "en")

    result.isFailure should be(true)
    result.failed.get.getMessage should equal ("Failed to save file")
  }

  test("uploadFiles should return an Audio objects if everything went ok") {
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(s3ObjectMock))
    val result = writeService.uploadFile(fileMock1, newAudioFile1.language)

    result.isSuccess should be (true)
    inside(result.get) { case Audio(filepath, mimetype, filesize, language) =>
      mimetype should equal ("audio/mp3")
      filesize should equal (1024)
      language should equal (newAudioFile1.language)
    }
  }

  test("storeNewAudio should return Failure if filetype is invalid") {
    when(fileMock1.contentType).thenReturn(Some("application/text"))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(Some(ValidationMessage("some-field", "some-message")))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(mock[ObjectMetadata]))

    writeService.storeNewAudio(newAudioMeta, fileMock1).isFailure should be (true)
  }

  test("storeNewAudio should return Failure if upload failes") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewAudio(newAudioMeta, fileMock1).isFailure should be (true)
  }

  test("storeNewAudio should return Failure if validation fails") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Failure(new ValidationException(errors=Seq())))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(mock[ObjectMetadata]))

    writeService.storeNewAudio(newAudioMeta, fileMock1).isFailure should be (true)
    verify(audioRepository, times(0)).insert(any[domain.AudioMetaInformation])(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("storeNewAudio should return Failure if failed to insert into database") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(mock[ObjectMetadata]))
    when(audioRepository.insert(any[domain.AudioMetaInformation])(any[DBSession])).thenThrow(new RuntimeException)

    writeService.storeNewAudio(newAudioMeta, fileMock1).isFailure should be (true)
    verify(searchIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("storeNewAudio should return Failure if failed to index audio metadata") {
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(mock[ObjectMetadata]))
    when(searchIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewAudio(newAudioMeta, fileMock1).isFailure should be (true)
    verify(audioRepository, times(1)).insert(any[domain.AudioMetaInformation])(any[DBSession])
  }

  test("storeNewAudio should return Success if creation of new audio file succeeded") {
    val afterInsert = domainAudioMeta.copy(id=Some(1), revision = Some(1))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(mock[ObjectMetadata]))
    when(searchIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))

    val result = writeService.storeNewAudio(newAudioMeta, fileMock1)
    result.isSuccess should be (true)
    result should equal(converterService.toApiAudioMetaInformation(afterInsert, Some(newAudioMeta.language)))

    verify(audioRepository, times(1)).insert(any[domain.AudioMetaInformation])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("that mergeAudioMeta overwrites fields from toUpdate for given language") {
    when(authUser.id()).thenReturn("ndla54321")

    val toUpdate = UpdatedAudioMetaInformation(1, "A new english title", "en", converterService.toApiCopyright(domainAudioMeta.copyright), Seq())
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate)
    merged.titles.length should be(1)
    merged.titles.head.title should equal ("A new english title")
  }

  test("that mergeAudioMeta adds fields from toUpdate for new language") {
    when(authUser.id()).thenReturn("ndla54321")

    val toUpdate = UpdatedAudioMetaInformation(1, "En ny norsk tittel", "nb", converterService.toApiCopyright(domainAudioMeta.copyright), Seq())
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate)
    merged.titles.length should be(2)
    merged.titles.filter(_.language.contains("nb")).head.title should equal ("En ny norsk tittel")
    merged.titles.filter(_.language.contains("en")).head.title should equal ("title")
  }

  test("that mergeAudioMeta does not merge filePaths if no new audio") {
    when(authUser.id()).thenReturn("ndla54321")

    val toUpdate = UpdatedAudioMetaInformation(1, "A new english title", "en", converterService.toApiCopyright(domainAudioMeta.copyright), Seq())
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate)
    merged.titles.length should be(1)
    merged.titles.head.title should equal ("A new english title")
    merged.filePaths should equal (domainAudioMeta.filePaths)
  }

  test("that mergeAudioMeta overwrites filepath if new audio for same language") {
    when(authUser.id()).thenReturn("ndla54321")

    val newAudio = Audio(newFileName2, "audio/mp3", 1024, "en")

    val toUpdate = UpdatedAudioMetaInformation(1, "A new english title", "en", converterService.toApiCopyright(domainAudioMeta.copyright), Seq())
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, Some(newAudio))
    merged.titles.length should be(1)
    merged.titles.head.title should equal ("A new english title")
    merged.filePaths.length should be (1)
    merged.filePaths.head.filePath should not equal domainAudioMeta.filePaths.head.filePath
    merged.filePaths.head.filePath should equal (newFileName2)
  }

  test("that mergeAudioMeta adds filepath if new audio for new language") {
    when(authUser.id()).thenReturn("ndla54321")

    val newAudio = Audio(newFileName2, "audio/mp3", 1024, "nb")

    val toUpdate = UpdatedAudioMetaInformation(1, "En ny norsk tittel", "nb", converterService.toApiCopyright(domainAudioMeta.copyright), Seq())
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, Some(newAudio))
    merged.titles.length should be(2)
    merged.filePaths.length should be (2)
    merged.filePaths.filter(_.language.contains("nb")).head.filePath should equal (newFileName2)
    merged.filePaths.filter(_.language.contains("en")).head.filePath should equal (domainAudioMeta.filePaths.head.filePath)
  }

  test("that updateAudio returns Failure when id is not found") {
    when(audioRepository.withId(1)).thenReturn(None)

    val result = writeService.updateAudio(1, updatedAudioMeta, None)
    result.isFailure should be (true)
    result.failed.get.getMessage should equal(new NotFoundException().getMessage)
  }

  test("that updateAudio returns Failure when audio file validation fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))

    val validationMessage = ValidationMessage("some-field", "This is an error")
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(Some(validationMessage))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(mock[FileItem]))
    result.isFailure should be (true)
    result.failed.get.getMessage should equal(new ValidationException(errors=Seq()).getMessage)
  }

  test("that updateAudio returns Failure when audio upload fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Failure(new RuntimeException("Something happened")))


    val result = writeService.updateAudio(1, updatedAudioMeta, Some(fileMock1))
    result.isFailure should be (true)
    result.failed.get.getMessage should equal("Something happened")
  }

  test("that updateAudio returns Failure when meta validation fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(s3ObjectMock))
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Failure(new ValidationException(errors=Seq())))


    val result = writeService.updateAudio(1, updatedAudioMeta, Some(fileMock1))
    result.isFailure should be (true)
    result.failed.get.getMessage should equal(new ValidationException(errors=Seq()).getMessage)

    verify(audioStorage, times(1)).deleteObject(any[String])
  }

  test("that updateAudio returns Failure when meta update fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(s3ObjectMock))
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioRepository.update(any[domain.AudioMetaInformation], any[Long])).thenThrow(new RuntimeException("Something happened"))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(fileMock1))
    result.isFailure should be (true)
    result.failed.get.getMessage should equal("Something happened")

    verify(audioStorage, times(1)).deleteObject(any[String])
  }

  test("that updateAudio returns Success when all is good and birds are singing") {
    val afterInsert = domainAudioMeta.copy(id=Some(1), revision = Some(1))

    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any[FileItem])).thenReturn(None)
    when(audioStorage.storeAudio(any[InputStream], any[String], any[Long], any[String])).thenReturn(Success(s3ObjectMock))
    when(validationService.validate(any[domain.AudioMetaInformation])).thenReturn(Success(domainAudioMeta))
    when(audioRepository.update(any[domain.AudioMetaInformation], any[Long])).thenReturn(Success(afterInsert))
    when(searchIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(fileMock1))
    result.isSuccess should be (true)

    verify(audioStorage, times(0)).deleteObject(any[String])
  }


}
