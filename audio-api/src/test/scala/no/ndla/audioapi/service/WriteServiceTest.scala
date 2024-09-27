/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.TestData.testUser
import no.ndla.audioapi.model.api.*
import no.ndla.audioapi.model.domain.{Audio, AudioType}
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model
import no.ndla.common.model.api.{Copyright, License}
import no.ndla.common.model.domain.UploadedFile
import no.ndla.common.model.{NDLADate, domain as common}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when, withSettings}
import org.mockito.invocation.InvocationOnMock
import org.mockito.quality.Strictness
import scalikejdbc.DBSession
import software.amazon.awssdk.services.s3.model.{DeleteObjectResponse, HeadObjectResponse, PutObjectResponse}

import java.io.FileInputStream
import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val writeService            = new WriteService
  override val converterService        = new ConverterService
  val (newFileName1, newFileName2)     = ("AbCdeF.mp3", "GhijKl.mp3")
  val filePartMock: UploadedFile       = mock[UploadedFile]
  val s3ObjectMock: HeadObjectResponse = mock[HeadObjectResponse]

  val newAudioMeta: NewAudioMetaInformation = NewAudioMetaInformation(
    "title",
    "en",
    Copyright(License("by", None, None), None, Seq(), Seq(), Seq(), None, None, false),
    Seq("tag"),
    None,
    None,
    None,
    None
  )

  val updatedAudioMeta: UpdatedAudioMetaInformation = UpdatedAudioMetaInformation(
    revision = 1,
    title = "title",
    language = "en",
    copyright = Copyright(License("by", None, None), None, Seq(), Seq(), Seq(), None, None, false),
    tags = Seq("tag"),
    audioType = None,
    podcastMeta = None,
    manuscript = None,
    seriesId = None
  )

  val updated: NDLADate       = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val created: model.NDLADate = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val someAudio: Audio = Audio(newFileName1, "audio/mp3", 1024, "en")

  val domainAudioMeta: domain.AudioMetaInformation =
    converterService.toDomainAudioMetaInformation(newAudioMeta, someAudio, None, testUser)
  val updated1: model.NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)

  val publicDomain: common.article.Copyright = common.article.Copyright(
    "publicdomain",
    Some("Metropolis"),
    List(common.Author("Forfatter", "Bruce Wayne")),
    Seq(),
    Seq(),
    None,
    None,
    false
  )

  val multiLangAudio: domain.AudioMetaInformation = domain.AudioMetaInformation(
    Some(4),
    Some(1),
    List(
      common.Title("Donald Duck kjører bil", "nb"),
      common.Title("Donald Duck kjører bil", "nn"),
      common.Title("Donald Duck drives a car", "en")
    ),
    List(
      domain.Audio("file1.mp3", "audio/mpeg", 1024, "nb"),
      domain.Audio("file2.mp3", "audio/mpeg", 1024, "nn"),
      domain.Audio("file3.mp3", "audio/mpeg", 1024, "en")
    ),
    publicDomain,
    List(
      common.Tag(List("and"), "nb"),
      common.Tag(List("and"), "nn"),
      common.Tag(List("duck"), "en")
    ),
    "ndla124",
    updated1,
    created,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None,
    None
  )

  override def beforeEach(): Unit = {
    when(filePartMock.fileName).thenReturn(Some("test.mp3"))
    when(filePartMock.contentType).thenReturn(Some("audio/mp3"))
    val fis = mock[FileInputStream]
    when(filePartMock.stream).thenReturn(fis)

    when(s3ObjectMock.contentLength()).thenReturn(1024)
    when(s3ObjectMock.contentType()).thenReturn("audio/mp3")

    reset(audioRepository)
    reset(audioIndexService)
    reset(tagIndexService)
    reset(s3Client)

    when(s3Client.deleteObject(any[String])).thenReturn(Success(mock[DeleteObjectResponse]))
    when(audioRepository.insert(any[domain.AudioMetaInformation])(any[DBSession]))
      .thenReturn(domainAudioMeta.copy(id = Some(1), revision = Some(1)))
  }

  test("converter to domain should set updatedBy from authUser and updated date") {
    when(clock.now()).thenReturn(updated)
    val domain =
      converterService.toDomainAudioMetaInformation(
        newAudioMeta,
        Audio(newFileName1, "audio/mp3", 1024, "en"),
        None,
        testUser
      )
    domain.updatedBy should equal("ndla54321")
    domain.updated should equal(updated)
  }

  test("randomFileName should return a random filename with a given length and extension") {
    val extension = ".mp3"

    val result = writeService.randomFileName(extension)
    result.length should be(12)
    result.endsWith(extension) should be(true)

    val resultWithNegativeLength = writeService.randomFileName(extension, -1)
    resultWithNegativeLength.length should be(1 + extension.length)
    resultWithNegativeLength.endsWith(extension) should be(true)
  }

  test("uploadFiles should return Failure if file upload failed") {
    when(s3Client.objectExists(any[String])).thenReturn(false)
    when(s3Client.putObject(any, any)).thenReturn(Failure(new RuntimeException))

    writeService.uploadFile(filePartMock, "en").isFailure should be(true)
  }

  test("deleteFiles should delete all files in a list") {
    writeService.deleteFile(Audio("mp3.mp3", "audio/mp3", 1024, "unknown"))
    verify(s3Client, times(1)).deleteObject(any[String])
  }

  test("uploadFile should return Failure if storeFile fails to upload") {
    when(s3Client.putObject(any, any))
      .thenReturn(Failure(new RuntimeException("Failed to save file")))

    val result = writeService.uploadFile(filePartMock, "en")

    result.isFailure should be(true)
    result.failed.get.getMessage should equal("Failed to save file")
  }

  test("uploadFiles should return an Audio objects if everything went ok") {
    when(s3Client.putObject(any, any)).thenReturn(Success(mock[PutObjectResponse]))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    val result = writeService.uploadFile(filePartMock, "nb")

    result.isSuccess should be(true)
    inside(result.get) { case Audio(_, mimetype, filesize, language) =>
      mimetype should equal("audio/mp3")
      filesize should equal(1024)
      language should equal("nb")
    }
  }

  test("storeNewAudio should return Failure if filetype is invalid") {
    when(validationService.validateAudioFile(any))
      .thenReturn(Seq(ValidationMessage("some-field", "some-message")))
    when(s3Client.putObject(any, any)).thenReturn(Success(mock[PutObjectResponse]))

    writeService.storeNewAudio(newAudioMeta, filePartMock, testUser).isFailure should be(true)
  }

  test("storeNewAudio should return Failure if upload failes") {
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(s3Client.putObject(any, any)).thenReturn(Failure(new RuntimeException))

    writeService.storeNewAudio(newAudioMeta, filePartMock, testUser).isFailure should be(true)
  }

  test("storeNewAudio should return Failure if validation fails") {
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Failure(new ValidationException(errors = Seq())))
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))

    writeService.storeNewAudio(newAudioMeta, filePartMock, testUser).isFailure should be(true)
    verify(audioRepository, times(0)).insert(any[domain.AudioMetaInformation])(any[DBSession])
    verify(audioIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
    verify(tagIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("storeNewAudio should return Failure if failed to insert into database") {
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Success(domainAudioMeta))
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(audioRepository.insert(any[domain.AudioMetaInformation])(any[DBSession])).thenThrow(new RuntimeException)

    writeService.storeNewAudio(newAudioMeta, filePartMock, testUser).isFailure should be(true)
    verify(audioIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
    verify(tagIndexService, times(0)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("storeNewAudio should return Failure if failed to index audio metadata") {
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Success(domainAudioMeta))
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(tagIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewAudio(newAudioMeta, filePartMock, testUser).isFailure should be(true)
    verify(audioRepository, times(1)).insert(any[domain.AudioMetaInformation])(any[DBSession])
  }

  test("storeNewAudio should return Success if creation of new audio file succeeded") {
    val afterInsert = domainAudioMeta.copy(id = Some(1), revision = Some(1))
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Success(domainAudioMeta))
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))
    when(audioRepository.setSeriesId(any, any)(any)).thenReturn(Success(1))

    val result = writeService.storeNewAudio(newAudioMeta, filePartMock, testUser)
    result.isSuccess should be(true)
    result should equal(converterService.toApiAudioMetaInformation(afterInsert, Some(newAudioMeta.language)))

    verify(audioRepository, times(1)).insert(any[domain.AudioMetaInformation])(any[DBSession])
    verify(audioIndexService, times(1)).indexDocument(any[domain.AudioMetaInformation])
    verify(tagIndexService, times(1)).indexDocument(any[domain.AudioMetaInformation])
  }

  test("that mergeAudioMeta overwrites fields from toUpdate for given language") {
    val toUpdate = UpdatedAudioMetaInformation(
      1,
      "A new english title",
      "en",
      converterService.toApiCopyright(domainAudioMeta.copyright),
      Seq(),
      None,
      None,
      None,
      None
    )
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, None, testUser).get
    merged.titles.length should be(1)
    merged.titles.head.title should equal("A new english title")
  }

  test("that mergeAudioMeta adds fields from toUpdate for new language") {
    val toUpdate = UpdatedAudioMetaInformation(
      1,
      "En ny norsk tittel",
      "nb",
      converterService.toApiCopyright(domainAudioMeta.copyright),
      Seq(),
      None,
      None,
      None,
      None
    )
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, None, testUser).get
    merged.titles.length should be(2)
    merged.titles.filter(_.language.contains("nb")).head.title should equal("En ny norsk tittel")
    merged.titles.filter(_.language.contains("en")).head.title should equal("title")
  }

  test("that mergeAudioMeta does not merge filePaths if no new audio") {
    val toUpdate = UpdatedAudioMetaInformation(
      1,
      "A new english title",
      "en",
      converterService.toApiCopyright(domainAudioMeta.copyright),
      Seq(),
      None,
      None,
      None,
      None
    )
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, None, testUser).get
    merged.titles.length should be(1)
    merged.titles.head.title should equal("A new english title")
    merged.filePaths should equal(domainAudioMeta.filePaths)
  }

  test("that mergeAudioMeta overwrites filepath if new audio for same language") {
    val newAudio = Audio(newFileName2, "audio/mp3", 1024, "en")

    val toUpdate = UpdatedAudioMetaInformation(
      1,
      "A new english title",
      "en",
      converterService.toApiCopyright(domainAudioMeta.copyright),
      Seq(),
      None,
      None,
      None,
      None
    )
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, Some(newAudio), testUser).get
    merged.titles.length should be(1)
    merged.titles.head.title should equal("A new english title")
    merged.filePaths.length should be(1)
    merged.filePaths.head.filePath should not equal domainAudioMeta.filePaths.head.filePath
    merged.filePaths.head.filePath should equal(newFileName2)
  }

  test("that mergeAudioMeta adds filepath if new audio for new language") {
    val newAudio = Audio(newFileName2, "audio/mp3", 1024, "nb")

    val toUpdate = UpdatedAudioMetaInformation(
      1,
      "En ny norsk tittel",
      "nb",
      converterService.toApiCopyright(domainAudioMeta.copyright),
      Seq(),
      None,
      None,
      None,
      None
    )
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, Some(newAudio), testUser).get
    merged.titles.length should be(2)
    merged.filePaths.length should be(2)
    merged.filePaths.filter(_.language.contains("nb")).head.filePath should equal(newFileName2)
    merged.filePaths.filter(_.language.contains("en")).head.filePath should equal(
      domainAudioMeta.filePaths.head.filePath
    )
  }

  test("that updateAudio returns Failure when id is not found") {
    when(audioRepository.withId(1)).thenReturn(None)

    val result = writeService.updateAudio(1, updatedAudioMeta, None, testUser)
    result.isFailure should be(true)
    result.failed.get.getMessage should equal(new NotFoundException().getMessage)
  }

  test("that updateAudio returns Failure when audio file validation fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))

    val validationMessage = ValidationMessage("some-field", "This is an error")
    when(validationService.validateAudioFile(any)).thenReturn(Seq(validationMessage))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(mock[UploadedFile]), testUser)
    result.isFailure should be(true)
    result.failed.get.getMessage should equal(new ValidationException(errors = Seq()).getMessage)
  }

  test("that updateAudio returns Failure when audio upload fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(s3Client.putObject(any, any))
      .thenReturn(Failure(new RuntimeException("Something happened")))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(filePartMock), testUser)
    result.isFailure should be(true)
    result.failed.get.getMessage should equal("Something happened")
  }

  test("that updateAudio returns Failure when meta validation fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Failure(new ValidationException(errors = Seq())))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(filePartMock), testUser)
    result.isFailure should be(true)
    result.failed.get.getMessage should equal(new ValidationException(errors = Seq()).getMessage)

    verify(s3Client, times(1)).deleteObject(any[String])
  }

  test("that updateAudio returns Failure when meta update fails") {
    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Success(domainAudioMeta))
    when(audioRepository.update(any[domain.AudioMetaInformation], any[Long]))
      .thenThrow(new RuntimeException("Something happened"))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(filePartMock), testUser)
    result.isFailure should be(true)
    result.failed.get.getMessage should equal("Something happened")

    verify(s3Client, times(1)).deleteObject(any[String])
  }

  test("that updateAudio returns Success when all is good and birds are singing") {
    val afterInsert = domainAudioMeta.copy(id = Some(1), revision = Some(1))

    when(audioRepository.withId(1)).thenReturn(Some(domainAudioMeta))
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(s3Client.putObject(any, any))
      .thenReturn(Success(mock[PutObjectResponse](withSettings.strictness(Strictness.LENIENT))))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Success(domainAudioMeta))
    when(audioRepository.update(any[domain.AudioMetaInformation], any[Long])).thenReturn(Success(afterInsert))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))
    when(audioRepository.setSeriesId(any, any)(any)).thenReturn(Success(1))

    val result = writeService.updateAudio(1, updatedAudioMeta, Some(filePartMock), testUser)
    result.isSuccess should be(true)

    verify(s3Client, times(1)).deleteObject(any[String])
    verify(s3Client, times(1)).deleteObject(newFileName1)
  }

  test("that deleting audio both deletes database entry, s3 object, and indexed document") {
    reset(audioRepository)
    reset(s3Client)
    reset(audioIndexService)

    val audioId = 4444.toLong

    when(audioRepository.withId(audioId)).thenReturn(Some(domainAudioMeta))
    when(audioRepository.deleteAudio(eqTo(audioId))(any[DBSession])).thenReturn(1)
    when(s3Client.deleteObject(any[String])).thenReturn(Success(mock[DeleteObjectResponse]))
    when(audioIndexService.deleteDocument(any[Long])).thenReturn(Success(audioId))

    writeService.deleteAudioAndFiles(audioId)

    verify(s3Client, times(1)).deleteObject(domainAudioMeta.filePaths.head.filePath)
    verify(audioIndexService, times(1)).deleteDocument(audioId)
    verify(audioRepository, times(1)).deleteAudio(eqTo(audioId))(any[DBSession])
  }

  test("That deleting language version deletes language") {
    reset(audioRepository)
    reset(s3Client)
    reset(audioIndexService)

    val audioId = 5555.toLong
    val audio = multiLangAudio.copy(
      id = Some(audioId),
      titles = List(
        common.Title("Donald Duck kjører bil", "nb"),
        common.Title("Donald Duck kjører bil", "nn"),
        common.Title("Donald Duck drives a car", "en")
      ),
      filePaths = List(
        domain.Audio("file1.mp3", "audio/mpeg", 1024, "nb"),
        domain.Audio("file2.mp3", "audio/mpeg", 1024, "nn"),
        domain.Audio("file3.mp3", "audio/mpeg", 1024, "en")
      ),
      tags = List(
        common.Tag(List("and"), "nb"),
        common.Tag(List("and"), "nn"),
        common.Tag(List("duck"), "en")
      ),
      manuscript = List(
        domain.Manuscript("Manuskript", "nb"),
        domain.Manuscript("Manuskript", "nn"),
        domain.Manuscript("Manuscript", "en")
      ),
      podcastMeta = List(
        domain.PodcastMeta("intro", domain.CoverPhoto("1", "alt"), "nb"),
        domain.PodcastMeta("intro", domain.CoverPhoto("1", "alt"), "nn"),
        domain.PodcastMeta("intro", domain.CoverPhoto("1", "alt"), "en")
      )
    )

    val expectedAudio = audio.copy(
      titles = List(
        common.Title("Donald Duck kjører bil", "nb"),
        common.Title("Donald Duck drives a car", "en")
      ),
      filePaths = List(
        domain.Audio("file1.mp3", "audio/mpeg", 1024, "nb"),
        domain.Audio("file3.mp3", "audio/mpeg", 1024, "en")
      ),
      tags = List(
        common.Tag(List("and"), "nb"),
        common.Tag(List("duck"), "en")
      ),
      manuscript = List(
        domain.Manuscript("Manuskript", "nb"),
        domain.Manuscript("Manuscript", "en")
      ),
      podcastMeta = List(
        domain.PodcastMeta("intro", domain.CoverPhoto("1", "alt"), "nb"),
        domain.PodcastMeta("intro", domain.CoverPhoto("1", "alt"), "en")
      )
    )

    when(audioRepository.withId(audioId)).thenReturn(Some(audio))
    when(s3Client.deleteObject(any)).thenReturn(Success(mock[DeleteObjectResponse]))
    when(audioRepository.update(any[domain.AudioMetaInformation], eqTo(audioId))).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.AudioMetaInformation](0))
    )
    when(audioRepository.setSeriesId(any[Long], any[Option[Long]])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.AudioMetaInformation](0)))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.AudioMetaInformation](0)))
    when(tagIndexService.indexDocument(any[domain.AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.AudioMetaInformation](0))
    )

    val updated = writeService.deleteAudioLanguageVersion(audioId, "nn")
    verify(audioRepository, times(1)).update(expectedAudio, audioId)
    updated.get.head.supportedLanguages should not contain ("nn")
  }

  test("That deleting last language version deletes entire image") {
    reset(audioRepository)
    reset(s3Client)
    reset(audioIndexService)

    val audioId = 5555.toLong
    val audio = multiLangAudio.copy(
      id = Some(audioId),
      titles = List(
        common.Title("Donald Duck drives a car", "en")
      ),
      filePaths = List(
        domain.Audio("file3.mp3", "audio/mpeg", 1024, "en")
      ),
      tags = List(
        common.Tag(List("duck"), "en")
      )
    )

    when(audioRepository.withId(audioId)).thenReturn(Some(audio))
    when(audioRepository.deleteAudio(eqTo(audioId))(any[DBSession])).thenReturn(1)
    when(s3Client.deleteObject(any[String])).thenReturn(Success(mock[DeleteObjectResponse]))
    when(audioIndexService.deleteDocument(any[Long])).thenReturn(Success(audioId))

    writeService.deleteAudioLanguageVersion(audioId, "en")

    verify(s3Client, times(1)).deleteObject(audio.filePaths.head.filePath)
    verify(audioIndexService, times(1)).deleteDocument(audioId)
    verify(audioRepository, times(1)).deleteAudio(eqTo(audioId))(any[DBSession])
  }

  def setupSuccessfulSeriesValidation(): Unit = {
    when(
      validationService.validatePodcastEpisodes(
        any[Seq[(Long, Option[domain.AudioMetaInformation])]],
        any[Option[Long]]
      )
    ).thenAnswer((i: InvocationOnMock) => {
      val inputArgument  = i.getArgument[Seq[(Long, Option[domain.AudioMetaInformation])]](0)
      val passedEpisodes = inputArgument.map(_._2.get)
      Success(passedEpisodes)
    })

    when(validationService.validate(any[domain.Series])).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.Series](0))
    })
  }

  test("That failed insertion does not updates episodes series id") {
    reset(validationService)
    reset(audioRepository)
    reset(seriesRepository)

    val series = TestData.SampleSeries

    setupSuccessfulSeriesValidation()
    when(seriesRepository.withId(any, any)).thenReturn(Success(Some(series)))
    when(audioRepository.withId(any[Long])).thenAnswer((i: InvocationOnMock) => {
      val id = i.getArgument[Long](0)
      series.episodes.get.find(_.id.contains(id))
    })
    when(seriesRepository.insert(any[domain.Series])(any[DBSession]))
      .thenReturn(Failure(new RuntimeException("weird failure there buddy")))

    val updateSeries = api.NewSeries(
      title = "nyTittel",
      description = "nyBeskrivelse",
      coverPhotoId = "555",
      coverPhotoAltText = "nyalt",
      episodes = Set(1),
      language = "nb",
      revision = Some(55),
      hasRSS = Some(true)
    )

    writeService.newSeries(updateSeries).isFailure should be(true)

    verify(audioRepository, times(0)).setSeriesId(any[Long], any[Option[Long]])(any[DBSession])
  }

  test("That failed updating does not updates episodes series id") {
    reset(validationService)
    reset(audioRepository)
    reset(seriesRepository)
    reset(audioIndexService)
    reset(seriesIndexService)

    val series = TestData.SampleSeries

    when(seriesRepository.withId(any, any)).thenReturn(Success(Some(series)))
    when(seriesIndexService.indexDocument(any[domain.Series])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )
    when(audioRepository.withId(any[Long])).thenAnswer((i: InvocationOnMock) => {
      val id = i.getArgument[Long](0)
      series.episodes.get.find(_.id.contains(id))
    })
    when(seriesRepository.update(any[domain.Series])(any[DBSession]))
      .thenReturn(Failure(new Helpers.OptimisticLockException))
    setupSuccessfulSeriesValidation()

    val updateSeries = api.NewSeries(
      title = "nyTittel",
      description = "nyBeskrivelse",
      coverPhotoId = "555",
      coverPhotoAltText = "nyalt",
      episodes = Set(1),
      language = "nb",
      revision = Some(55),
      hasRSS = None
    )

    writeService.updateSeries(1, updateSeries).isFailure should be(true)

    verify(audioRepository, times(0)).setSeriesId(any[Long], any[Option[Long]])(any[DBSession])
  }

  test("That successful insertion updates episodes series id") {
    reset(validationService)
    reset(audioRepository)
    reset(seriesRepository)
    reset(audioIndexService)
    reset(seriesIndexService)

    val series = TestData.SampleSeries

    setupSuccessfulSeriesValidation()
    when(seriesIndexService.indexDocument(any[domain.Series])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )
    when(seriesRepository.withId(any, any)).thenReturn(Success(Some(series)))
    when(audioRepository.withId(any[Long])).thenAnswer((i: InvocationOnMock) => {
      val id = i.getArgument[Long](0)
      series.episodes.get.find(_.id.contains(id))
    })
    when(audioRepository.setSeriesId(any[Long], any[Option[Long]])(any[DBSession])).thenReturn(Success(1))

    when(seriesRepository.insert(any[domain.SeriesWithoutId])(any[DBSession]))
      .thenAnswer((i: InvocationOnMock) => {
        Success(
          domain.Series.fromId(
            id = 1,
            revision = 1,
            i.getArgument[domain.SeriesWithoutId](0)
          )
        )
      })

    val updateSeries = api.NewSeries(
      title = "nyTittel",
      description = "nyBeskrivelse",
      coverPhotoId = "555",
      coverPhotoAltText = "nyalt",
      episodes = Set(1),
      language = "nb",
      revision = Some(55),
      hasRSS = Some(false)
    )

    val result = writeService.newSeries(updateSeries)
    result.isSuccess should be(true)

    verify(audioRepository, times(1)).setSeriesId(any[Long], any[Option[Long]])(any[DBSession])

  }

  test("That successful updating updates episodes series id") {
    reset(validationService)
    reset(audioRepository)
    reset(seriesRepository)
    reset(audioIndexService)
    reset(seriesIndexService)

    val episodesMap = Map(
      1L -> TestData.samplePodcast.copy(id = Some(1)),
      2L -> TestData.samplePodcast.copy(id = Some(2))
    )

    val series = TestData.SampleSeries

    setupSuccessfulSeriesValidation()
    when(seriesIndexService.indexDocument(any[domain.Series])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )
    when(seriesRepository.withId(any, any)).thenReturn(Success(Some(series)))
    when(audioRepository.withId(any[Long])).thenAnswer((i: InvocationOnMock) => {
      val id = i.getArgument[Long](0)
      episodesMap.get(id)
    })
    when(audioRepository.setSeriesId(any[Long], any[Option[Long]])(any[DBSession])).thenReturn(Success(1))
    when(seriesRepository.update(any[domain.Series])(any[DBSession])).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.Series](0))
    })
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument(0))
    )

    val updateSeries = api.NewSeries(
      title = "nyTittel",
      description = "nyBeskrivelse",
      coverPhotoId = "555",
      coverPhotoAltText = "nyalt",
      episodes = Set(2),
      language = "nb",
      revision = Some(55),
      hasRSS = Some(true)
    )

    val result = writeService.updateSeries(1, updateSeries)
    result.isSuccess should be(true)

    verify(audioRepository, times(1)).setSeriesId(eqTo(1L), eqTo(None))(any[DBSession])
    verify(audioRepository, times(1)).setSeriesId(eqTo(2L), eqTo(Some(1L)))(any[DBSession])
  }

  test("That successful updating doesnt update existing episodes") {
    reset(validationService)
    reset(audioRepository)
    reset(seriesRepository)

    val episodesMap = Map(
      1L -> TestData.samplePodcast.copy(id = Some(1))
    )

    val series = TestData.SampleSeries

    setupSuccessfulSeriesValidation()
    when(seriesRepository.withId(any, any)).thenReturn(Success(Some(series)))
    when(audioRepository.withId(any[Long])).thenAnswer((i: InvocationOnMock) => {
      val id = i.getArgument[Long](0)
      episodesMap.get(id)
    })
    when(audioRepository.setSeriesId(any[Long], any[Option[Long]])(any[DBSession])).thenReturn(Success(1))
    when(seriesRepository.update(any[domain.Series])(any[DBSession])).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.Series](0))
    })

    val updateSeries = api.NewSeries(
      title = "nyTittel",
      description = "nyBeskrivelse",
      coverPhotoId = "555",
      coverPhotoAltText = "nyalt",
      episodes = Set(1),
      language = "nb",
      revision = Some(55),
      hasRSS = Some(true)
    )

    val result = writeService.updateSeries(1, updateSeries)
    result.isSuccess should be(true)

    verify(audioRepository, times(0)).setSeriesId(any[Long], any[Option[Long]])(any[DBSession])
  }

  test("that mergeAudioMeta removes duplicate tags from toUpdate for given language") {
    val toUpdate = UpdatedAudioMetaInformation(
      1,
      "A new english title",
      "en",
      converterService.toApiCopyright(domainAudioMeta.copyright),
      Seq("abc", "123", "abc", "def"),
      None,
      None,
      None,
      None
    )
    val (merged, _) = writeService.mergeAudioMeta(domainAudioMeta, toUpdate, None, testUser).get
    merged.tags.length should be(1)
    merged.tags.head.tags should equal(Seq("abc", "123", "def"))
  }

  test("That deleting last language version of a file deletes it") {
    reset(audioRepository)
    reset(s3Client)
    reset(audioIndexService)

    val audioId = 5555.toLong
    val audio = multiLangAudio.copy(
      id = Some(audioId),
      titles = List(
        common.Title("Donald Duck kjører bil", "nb"),
        common.Title("Donald Duck kjører bil", "nn"),
        common.Title("Donald Duck drives a car", "en")
      ),
      filePaths = List(
        domain.Audio("file1.mp3", "audio/mpeg", 1024, "nb"),
        domain.Audio("file2.mp3", "audio/mpeg", 1024, "nn"),
        domain.Audio("file3.mp3", "audio/mpeg", 1024, "en")
      ),
      tags = List(
        common.Tag(List("and"), "nb"),
        common.Tag(List("and"), "nn"),
        common.Tag(List("duck"), "en")
      )
    )

    when(audioRepository.withId(audioId)).thenReturn(Some(audio))
    when(s3Client.deleteObject(any[String])).thenReturn(Success(mock[DeleteObjectResponse]))
    when(audioRepository.update(any[domain.AudioMetaInformation], eqTo(audioId))).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.AudioMetaInformation](0))
    )
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.AudioMetaInformation](0)))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.AudioMetaInformation](0)))

    writeService.deleteAudioLanguageVersion(audioId, "nn")

    verify(s3Client, times(1)).deleteObject("file2.mp3")
    verify(s3Client, times(1)).deleteObject(any[String])

  }

  test("That deleting language version will not delete file if other languages use it") {
    reset(audioRepository)
    reset(s3Client)
    reset(audioIndexService)

    val audioId = 5555.toLong
    val audio = multiLangAudio.copy(
      id = Some(audioId),
      titles = List(
        common.Title("Donald Duck kjører bil", "nb"),
        common.Title("Donald Duck kjører bil", "nn"),
        common.Title("Donald Duck drives a car", "en")
      ),
      filePaths = List(
        domain.Audio("file1.mp3", "audio/mpeg", 1024, "nb"),
        domain.Audio("file2.mp3", "audio/mpeg", 1024, "nn"),
        domain.Audio("file2.mp3", "audio/mpeg", 1024, "en")
      ),
      tags = List(
        common.Tag(List("and"), "nb"),
        common.Tag(List("and"), "nn"),
        common.Tag(List("duck"), "en")
      )
    )

    when(audioRepository.withId(audioId)).thenReturn(Some(audio))
    when(audioRepository.update(any[domain.AudioMetaInformation], eqTo(audioId))).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.AudioMetaInformation](0))
    )
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.AudioMetaInformation](0)))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.AudioMetaInformation](0)))

    writeService.deleteAudioLanguageVersion(audioId, "nn")

    verify(s3Client, times(0)).deleteObject(any[String])

  }

  test("That when uploading a new file, remove the old file if not used in other language version") {
    reset(audioRepository)
    reset(s3Client)
    reset(audioIndexService)

    val audioId = 5555.toLong
    val audio = multiLangAudio.copy(
      id = Some(audioId),
      titles = List(
        common.Title("Donald Duck kjører bil", "nb"),
        common.Title("Donald Duck kjører bil", "nn"),
        common.Title("Donald Duck drives a car", "en")
      ),
      filePaths = List(
        domain.Audio("file1.mp3", "audio/mpeg", 1024, "nb"),
        domain.Audio("file2.mp3", "audio/mpeg", 1024, "nn"),
        domain.Audio("file3.mp3", "audio/mpeg", 1024, "en")
      ),
      tags = List(
        common.Tag(List("and"), "nb"),
        common.Tag(List("and"), "nn"),
        common.Tag(List("duck"), "en")
      )
    )

    val afterInsert = audio.copy(id = Some(5555), revision = Some(1))

    when(s3Client.deleteObject(any)).thenReturn(Success(mock[DeleteObjectResponse]))
    when(audioRepository.withId(5555)).thenReturn(Some(audio))
    when(validationService.validateAudioFile(any)).thenReturn(Seq.empty)
    when(s3Client.putObject(any, any)).thenReturn(Success(mock[PutObjectResponse]))
    when(s3Client.headObject(any)).thenReturn(Success(s3ObjectMock))
    when(
      validationService.validate(
        any[domain.AudioMetaInformation],
        any[Option[domain.AudioMetaInformation]],
        any[Option[domain.Series]],
        any[Option[String]]
      )
    )
      .thenReturn(Success(audio))
    when(audioRepository.update(any[domain.AudioMetaInformation], any[Long])).thenReturn(Success(afterInsert))
    when(audioIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[domain.AudioMetaInformation])).thenReturn(Success(afterInsert))
    when(audioRepository.setSeriesId(any, any)(any)).thenReturn(Success(5555))

    val result = writeService.updateAudio(5555, updatedAudioMeta, Some(filePartMock), testUser)
    result.isSuccess should be(true)

    verify(s3Client, times(1)).deleteObject("file3.mp3")

  }

}
