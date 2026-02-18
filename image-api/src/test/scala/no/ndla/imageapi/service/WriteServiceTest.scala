/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.errors.ValidationException
import no.ndla.common.model.api.{CopyrightDTO, LicenseDTO, Missing, UpdateWith}
import no.ndla.common.model.domain.article.Copyright as DomainCopyright
import no.ndla.common.model.domain.{ContributorType, UploadedFile}
import no.ndla.common.model.{NDLADate, api as commonApi, domain as common}
import no.ndla.imageapi.model.api.*
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.domain.{ImageMetaInformation, ImageVariantSize, ModelReleasedStatus}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override implicit lazy val writeService: WriteService         = new WriteService
  override implicit lazy val converterService: ConverterService = new ConverterService
  val newFileName                                               = "AbCdeF.mp3"
  val fileMock1: UploadedFile                                   = mock[UploadedFile]

  val newImageMeta: NewImageMetaInformationV2DTO = NewImageMetaInformationV2DTO(
    "title",
    Some("alt text"),
    CopyrightDTO(LicenseDTO("by", None, None), None, Seq.empty, Seq.empty, Seq.empty, None, None, false),
    Seq.empty,
    "",
    "en",
    Some(ModelReleasedStatus.YES.toString),
  )
  val userId                        = "ndla124"
  val userWithWriteScope: TokenUser = TokenUser(userId, Set(IMAGE_API_WRITE), None)

  def updated(): NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)

  val domainImageMeta: ImageMetaInformation = converterService
    .asDomainImageMetaInformationV2(newImageMeta, TokenUser.SystemUser)
    .get

  val multiLangImage = new ImageMetaInformation(
    id = Some(2),
    titles =
      List(domain.ImageTitle("nynorsk", "nn"), domain.ImageTitle("english", "en"), domain.ImageTitle("norsk", "und")),
    alttexts = List(),
    images = Seq(
      new domain.ImageFileData(
        fileName = "yolo.jpeg",
        size = 100,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = DomainCopyright("", None, List(), List(), List(), None, None, false),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  override def beforeEach(): Unit = {
    when(fileMock1.contentType).thenReturn(Some("image/jpeg"))
    val imageStream = TestData.ndlaLogoImageStream
    when(fileMock1.createStream()).thenReturn(imageStream.stream)
    when(fileMock1.fileSize).thenReturn(imageStream.contentLength)
    when(fileMock1.fileName).thenReturn(Some("file.jpg"))
    when(random.string(any)).thenCallRealMethod()

    reset(imageRepository)
    reset(imageIndexService)
    reset(imageStorage)
    reset(tagIndexService)
    when(imageRepository.insert(any[ImageMetaInformation])(using any[DBSession])).thenReturn(
      Success(domainImageMeta.copy(id = Some(1L)))
    )
  }

  test("uploadImageWithVariants should return Success if file upload succeeds") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Success(newFileName))
    val expectedImage = domain.UploadedImage(
      newFileName,
      fileMock1.fileSize,
      "image/jpeg",
      Some(domain.ImageDimensions(189, 60)),
      Seq.empty,
    )

    val result = writeService.uploadImageWithVariants(fileMock1).failIfFailure
    verify(imageStorage, times(1)).uploadFromStream(any, any, any, any)

    result should equal(expectedImage)
  }

  test("uploadImageWithVariants should return Failure if file upload failed") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Failure(new RuntimeException))

    writeService.uploadImageWithVariants(fileMock1).isFailure should be(true)
  }

  test("storeNewImage should return Failure if upload failes") {
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Failure(new RuntimeException))
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[ImageMetaInformation](0))
    })

    writeService.storeNewImage(newImageMeta, fileMock1, TokenUser.SystemUser).isFailure should be(true)
  }

  test("storeNewImage should return Failure if validation fails") {
    reset(imageRepository, imageIndexService, imageStorage)
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(
      Failure(new ValidationException(errors = Seq()))
    )
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Success(newFileName))
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))

    writeService.storeNewImage(newImageMeta, fileMock1, TokenUser.SystemUser).isFailure should be(true)
    verify(imageRepository, times(0)).insert(any[ImageMetaInformation])(using any[DBSession])
    verify(imageIndexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
  }

  test("storeNewImage should return Failure if failed to insert into database") {
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Success(newFileName))
    when(imageRepository.insert(any[ImageMetaInformation])(using any[DBSession])).thenReturn(
      Failure(new RuntimeException)
    )
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))

    writeService.storeNewImage(newImageMeta, fileMock1, TokenUser.SystemUser).isFailure should be(true)
    verify(imageIndexService, times(0)).indexDocument(any[ImageMetaInformation])
    // Should upload, but then delete due to DB failure
    verify(imageStorage, times(1)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(1)).deleteObject(any)
  }

  test("storeNewImage should return Failure if failed to index image metadata") {
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))
    when(imageStorage.deleteObjects(any)).thenReturn(Success(()))
    when(imageRepository.insert(any)(using any)).thenReturn(Success(domainImageMeta.copy(id = Some(100L))))

    val result = writeService.storeNewImage(newImageMeta, fileMock1, TokenUser.SystemUser)
    result.isFailure should be(true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(using any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to index tag metadata") {
    val afterInsert = domainImageMeta.copy(id = Some(1))
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))
    when(imageStorage.deleteObjects(any)).thenReturn(Success(()))
    when(imageRepository.insert(any)(using any)).thenReturn(Success(afterInsert))

    writeService.storeNewImage(newImageMeta, fileMock1, TokenUser.SystemUser).isFailure should be(true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(using any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
    verify(imageIndexService, times(1)).deleteDocument(eqTo(1L))
  }

  test("storeNewImage should return Success if creation of new image file succeeded") {
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenAnswer(i => Success(i.getArgument(0)))
    when(imageStorage.uploadFromStream(any, any, any, any)).thenReturn(Success(newFileName))
    when(imageRepository.insert(any[ImageMetaInformation])(using any)).thenAnswer(i =>
      Success(i.getArgument(0).asInstanceOf[ImageMetaInformation].copy(id = Some(1)))
    )
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenAnswer(i => Success(i.getArgument(0)))
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenAnswer(i => Success(i.getArgument(0)))
    val expectedImageFile = domain.ImageFileData(
      newFileName,
      fileMock1.fileSize,
      "image/jpeg",
      Some(domain.ImageDimensions(189, 60)),
      Seq.empty,
      "en",
    )
    val expectedImageMeta = domainImageMeta.copy(id = Some(1), images = Seq(expectedImageFile))

    val result = writeService.storeNewImage(newImageMeta, fileMock1, TokenUser.SystemUser).failIfFailure

    result should be(expectedImageMeta)

    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(using any[DBSession])
    verify(imageIndexService, times(1)).indexDocument(any[ImageMetaInformation])
    verify(tagIndexService, times(1)).indexDocument(any[ImageMetaInformation])
  }

  test("getFileExtension returns the extension") {
    writeService.getFileExtension("image.jpg") should equal(Some(".jpg"))
    writeService.getFileExtension("ima.ge.jpg") should equal(Some(".jpg"))
    writeService.getFileExtension(".jpeg") should equal(Some(".jpeg"))
  }

  test("getFileExtension returns None if no extension was found") {
    writeService.getFileExtension("image-jpg") should equal(None)
    writeService.getFileExtension("jpeg") should equal(None)
  }

  test("converter to domain should set updatedBy from authUser and updated date") {
    val tokenUser = TokenUser("ndla54321", Set(IMAGE_API_WRITE), None)
    when(clock.now()).thenReturn(updated())
    val domain = converterService.asDomainImageMetaInformationV2(newImageMeta, tokenUser).get
    domain.updatedBy should equal("ndla54321")
    domain.updated should equal(updated())
  }

  test("mergeImageMeta should append a new language if language not already exists") {
    val date = NDLADate.now()
    when(clock.now()).thenReturn(date)
    val user          = "ndla124"
    val existing      = TestData.elg.copy(updated = date, updatedBy = user)
    val existingImage = existing.images.head
    val toUpdate      =
      UpdateImageMetaInformationDTO("en", Some("Title"), UpdateWith("AltText"), None, None, None, None, None)
    val expectedResult = existing.copy(
      titles = List(existing.titles.head, domain.ImageTitle("Title", "en")),
      images = List(existingImage, existingImage.copy(language = "en")),
      alttexts = List(existing.alttexts.head, domain.ImageAltText("AltText", "en")),
      editorNotes = Seq(domain.EditorNote(date, user, "Added new language 'en'.")),
    )

    val result = writeService.mergeImageMeta(existing, toUpdate, userWithWriteScope).failIfFailure

    result should be(expectedResult)
  }

  test("mergeImageMeta overwrite a languages if specified language already exist in cover") {
    val date = NDLADate.now()
    when(clock.now()).thenReturn(date)
    val user     = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate =
      UpdateImageMetaInformationDTO("nb", Some("Title"), UpdateWith("AltText"), None, None, None, None, None)
    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      editorNotes = Seq(domain.EditorNote(date, user, "Updated image data.")),
    )

    val result = writeService.mergeImageMeta(existing, toUpdate, userWithWriteScope).failIfFailure

    result should be(expectedResult)
  }

  test("mergeImageMeta updates optional values if specified") {
    val date = NDLADate.now()
    when(clock.now()).thenReturn(date)
    val user     = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformationDTO(
      "nb",
      Some("Title"),
      UpdateWith("AltText"),
      Some(
        CopyrightDTO(
          LicenseDTO("testLic", Some("License for testing"), None),
          Some("test"),
          List(commonApi.AuthorDTO(ContributorType.Originator, "Testerud")),
          List(),
          List(),
          None,
          None,
          false,
        )
      ),
      Some(List("a", "b", "c")),
      Some("Caption"),
      Some(ModelReleasedStatus.NO.toString),
      Some(true),
    )
    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      copyright = DomainCopyright(
        "testLic",
        Some("test"),
        List(common.Author(ContributorType.Originator, "Testerud")),
        List(),
        List(),
        None,
        None,
        false,
      ),
      tags = List(common.Tag(List("a", "b", "c"), "nb")),
      captions = List(domain.ImageCaption("Caption", "nb")),
      modelReleased = ModelReleasedStatus.NO,
      editorNotes = Seq(domain.EditorNote(date, "ndla124", "Updated image data.")),
      inactive = true,
    )

    val result = writeService.mergeImageMeta(existing, toUpdate, userWithWriteScope).failIfFailure

    result should be(expectedResult)
  }

  test("mergeImageMeta adds imagefile for language if it doesn't exist already") {
    val date = NDLADate.now()
    when(clock.now()).thenReturn(date)
    val imageId = 1L
    val user    = "ndla124"
    val image   = domain.ImageFileData(
      fileName = "yo.jpg",
      size = 123,
      contentType = "image/jpeg",
      dimensions = Some(domain.ImageDimensions(10, 10)),
      variants = Seq.empty,
      language = "nb",
    )
    val existing = TestData
      .elg
      .copy(
        id = Some(imageId),
        titles = Seq(domain.ImageTitle("yo", "nb"), domain.ImageTitle("hey", "nn")),
        updated = date,
        updatedBy = user,
        images = Seq(image),
      )
    val toUpdate       = UpdateImageMetaInformationDTO("nn", None, Missing, None, None, None, None, None)
    val expectedResult = existing.copy(images = Seq(image, image.copy(language = "nn")))

    val result = writeService.mergeImageMeta(existing, toUpdate, userWithWriteScope).failIfFailure

    result should be(expectedResult)
  }

  test("that deleting image deletes database entry, s3 object, and indexed document") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val imageId         = 4444.toLong
    val domainWithImage = domainImageMeta.copy(images =
      Seq(domain.ImageFileData(newFileName, 1024, "image/jpeg", Some(domain.ImageDimensions(189, 60)), Seq.empty, "nb"))
    )

    when(imageRepository.withId(imageId)).thenReturn(Success(Some(domainWithImage)))
    when(imageRepository.delete(eqTo(imageId))(using any[DBSession])).thenReturn(Success(1))
    when(imageStorage.deleteObject(any[String])).thenReturn(Success(()))
    when(imageStorage.deleteObjects(any)).thenReturn(Success(()))
    when(imageIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Long](0))
    )
    when(tagIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Long](0)))

    writeService.deleteImageAndFiles(imageId)

    verify(imageStorage, times(1)).deleteObject(domainWithImage.images.head.fileName)
    verify(imageIndexService, times(1)).deleteDocument(imageId)
    verify(tagIndexService, times(1)).deleteDocument(imageId)
    verify(imageRepository, times(1)).delete(eqTo(imageId))(using any[DBSession])
  }

  test("That deleting language version deletes language") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val date = NDLADate.now()
    val user = "ndla124"

    when(clock.now()).thenReturn(date)

    val imageId       = 5555.toLong
    val image         = multiLangImage.copy(id = Some(imageId))
    val expectedImage = image.copy(
      titles = List(domain.ImageTitle("english", "en"), domain.ImageTitle("norsk", "und")),
      editorNotes = image.editorNotes :+ domain.EditorNote(date, user, "Deleted language 'nn'."),
    )

    when(imageRepository.withId(imageId)).thenReturn(Success(Some(image)))
    when(imageRepository.update(any[ImageMetaInformation], eqTo(imageId))(using any)).thenAnswer(
      (i: InvocationOnMock) => Success(i.getArgument[ImageMetaInformation](0))
    )
    when(validationService.validate(any[ImageMetaInformation], any[Option[ImageMetaInformation]])).thenAnswer(
      (i: InvocationOnMock) => Success(i.getArgument[ImageMetaInformation](0))
    )
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[ImageMetaInformation](0))
    )
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[ImageMetaInformation](0))
    )

    writeService.deleteImageLanguageVersionV2(imageId, "nn", userWithWriteScope)

    verify(imageRepository, times(1)).update(eqTo(expectedImage), eqTo(imageId))(using any)
  }

  test("That deleting last language version deletes entire image") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val imageId = 6666.toLong
    val image   = multiLangImage.copy(
      id = Some(imageId),
      titles = List(domain.ImageTitle("english", "en")),
      captions = List(domain.ImageCaption("english", "en")),
      tags = Seq(common.Tag(Seq("eng", "elsk"), "en")),
      alttexts = Seq(domain.ImageAltText("english", "en")),
      images = Seq(TestData.bjorn.images.head.copy(language = "en")),
    )

    when(imageRepository.withId(imageId)).thenReturn(Success(Some(image)))
    when(imageRepository.delete(eqTo(imageId))(using any[DBSession])).thenReturn(Success(1))
    when(imageStorage.deleteObject(any[String])).thenReturn(Success(()))
    when(imageStorage.deleteObjects(any)).thenReturn(Success(()))
    when(imageIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Long](0))
    )
    when(tagIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Long](0)))

    writeService.deleteImageLanguageVersionV2(imageId, "en", userWithWriteScope)

    verify(imageStorage, times(1)).deleteObject(image.images.head.fileName)
    verify(imageIndexService, times(1)).deleteDocument(imageId)
    verify(tagIndexService, times(1)).deleteDocument(imageId)
    verify(imageRepository, times(1)).delete(eqTo(imageId))(using any[DBSession])
  }

  test("That updating image file with multiple same filepaths does not override filepath") {
    reset(validationService)
    reset(imageRepository)
    reset(imageStorage)
    val imageId  = 100L
    val coolDate = NDLADate.now()
    val upd      = UpdateImageMetaInformationDTO(
      language = "nb",
      title = Some("new title"),
      alttext = Missing,
      copyright = None,
      tags = None,
      caption = None,
      modelReleased = None,
      inactive = None,
    )
    val image = domain.ImageFileData(
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpeg",
      dimensions = None,
      variants = Seq.empty,
      language = "nb",
    )

    val dbImage = TestData
      .bjorn
      .copy(
        images = Seq(image.copy(language = "nn"), image.copy(language = "nb")),
        updated = coolDate,
        updatedBy = "ndla124",
      )

    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Success(Some(dbImage)))
    when(imageRepository.update(any, any)(using any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageStorage.moveObjects(any)).thenReturn(Success(()))
    when(imageStorage.uploadFromStream(any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[String](0))
    })
    when(imageIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(tagIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(clock.now()).thenReturn(coolDate)
    when(imageStorage.objectExists(any)).thenReturn(false)
    when(random.string(any)).thenReturn("randomstring")

    val expectedResult = dbImage.copy(
      titles = Seq(domain.ImageTitle("new title", "nb")),
      images = Seq(
        image.copy(language = "nn"),
        image.copy(
          fileName = "randomstring.jpg",
          size = fileMock1.fileSize,
          dimensions = Some(domain.ImageDimensions(189, 60)),
          language = "nb",
        ),
      ),
      editorNotes = List(
        domain.EditorNote(coolDate, "ndla124", "Updated image file for 'nb' language."),
        domain.EditorNote(coolDate, "ndla124", "Updated image data."),
      ),
    )

    val result = writeService.updateImageAndFile(imageId, upd, Some(fileMock1), userWithWriteScope).failIfFailure
    result should be(expectedResult)

    verify(imageStorage, times(1)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
    verify(imageStorage, times(0)).moveObjects(any)
    verify(imageRepository, times(1)).update(any, any)(using any)
  }

  test("That uploading image for a new language just adds a new one") {
    reset(validationService)
    reset(imageRepository)
    reset(imageStorage)
    val imageId  = 100L
    val coolDate = NDLADate.now()
    val upd      = UpdateImageMetaInformationDTO(
      language = "nb",
      title = Some("new title"),
      alttext = Missing,
      copyright = None,
      tags = None,
      caption = None,
      modelReleased = None,
      inactive = None,
    )
    val image = domain.ImageFileData(
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpeg",
      dimensions = None,
      variants = Seq.empty,
      language = "nb",
    )

    val dbImage = TestData
      .bjorn
      .copy(images = Seq(image.copy(language = "nn")), updated = coolDate, updatedBy = "ndla124")

    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Success(Some(dbImage)))
    when(imageRepository.update(any, any)(using any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageStorage.moveObjects(any)).thenReturn(Success(()))
    when(imageStorage.uploadFromStream(any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[String](0))
    })
    when(imageIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(tagIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(clock.now()).thenReturn(coolDate)
    when(imageStorage.objectExists(any)).thenReturn(false)
    when(random.string(any)).thenReturn("randomstring")

    val expectedResult = dbImage.copy(
      titles = Seq(domain.ImageTitle("new title", "nb")),
      images = Seq(
        image.copy(language = "nn"),
        image.copy(
          fileName = "randomstring.jpg",
          size = fileMock1.fileSize,
          dimensions = Some(domain.ImageDimensions(189, 60)),
          language = "nb",
        ),
      ),
      editorNotes = List(
        domain.EditorNote(coolDate, "ndla124", "Updated image file for 'nb' language."),
        domain.EditorNote(coolDate, "ndla124", "Updated image data."),
      ),
    )

    val result = writeService.updateImageAndFile(imageId, upd, Some(fileMock1), userWithWriteScope).failIfFailure
    result should be(expectedResult)

    verify(imageStorage, times(1)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
    verify(imageStorage, times(0)).moveObjects(any)
    verify(imageRepository, times(1)).update(any, any)(using any)
  }

  test("Deleting language version should delete file if only used by that language") {
    reset(validationService)
    reset(imageRepository)
    reset(imageStorage)
    val imageId  = 100L
    val coolDate = NDLADate.now()

    val image = domain.ImageFileData(
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpeg",
      dimensions = None,
      variants = Seq.empty,
      language = "nb",
    )

    val dbImage = TestData
      .bjorn
      .copy(
        titles = Seq(domain.ImageTitle("hei nb", "nb"), domain.ImageTitle("hei nn", "nn")),
        images = Seq(
          image.copy(fileName = "hello-nb.jpg", language = "nb"),
          image.copy(fileName = "hello-nn.jpg", language = "nn"),
        ),
        updated = coolDate,
        updatedBy = "ndla124",
      )

    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Success(Some(dbImage)))
    when(imageRepository.update(any, any)(using any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageStorage.uploadFromStream(any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[String](1))
    })
    when(imageIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(tagIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(clock.now()).thenReturn(coolDate)
    when(imageStorage.objectExists(any)).thenReturn(false)
    when(random.string(any)).thenReturn("randomstring")
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))
    when(imageStorage.deleteObjects(any)).thenReturn(Success(()))

    val expectedResult = dbImage.copy(
      titles = Seq(domain.ImageTitle("hei nb", "nb")),
      images = Seq(image.copy(fileName = "hello-nb.jpg", language = "nb")),
      editorNotes = Seq(domain.EditorNote(coolDate, "ndla124", "Deleted language 'nn'.")),
    )

    val result = writeService.deleteImageLanguageVersion(imageId, "nn", userWithWriteScope)
    result should be(Success(Some(expectedResult)))

    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(1)).deleteObject(eqTo("hello-nn.jpg"))
    verify(imageRepository, times(1)).update(any, any)(using any)
  }

  test("Deleting language version should not delete file if it used by more languages") {
    reset(validationService)
    reset(imageRepository)
    reset(imageStorage)
    val imageId  = 100L
    val coolDate = NDLADate.now()

    val image = domain.ImageFileData(
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpeg",
      dimensions = None,
      variants = Seq.empty,
      language = "nb",
    )

    val dbImage = TestData
      .bjorn
      .copy(
        titles = Seq(domain.ImageTitle("hei nb", "nb"), domain.ImageTitle("hei nn", "nn")),
        images = Seq(
          image.copy(fileName = "hello-shared.jpg", language = "nb"),
          image.copy(fileName = "hello-shared.jpg", language = "nn"),
        ),
        updated = coolDate,
        updatedBy = "ndla124",
      )

    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Success(Some(dbImage)))
    when(imageRepository.update(any, any)(using any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageStorage.uploadFromStream(any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[String](1))
    })
    when(imageIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(tagIndexService.indexDocument(any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(clock.now()).thenReturn(coolDate)
    when(imageStorage.objectExists(any)).thenReturn(false)
    when(random.string(any)).thenReturn("randomstring")
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))

    val expectedResult = dbImage.copy(
      titles = Seq(domain.ImageTitle("hei nb", "nb")),
      images = Seq(image.copy(fileName = "hello-shared.jpg", language = "nb")),
      editorNotes = Seq(domain.EditorNote(coolDate, "ndla124", "Deleted language 'nn'.")),
    )

    val result = writeService.deleteImageLanguageVersion(imageId, "nn", userWithWriteScope)
    result should be(Success(Some(expectedResult)))

    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
    verify(imageRepository, times(1)).update(any, any)(using any)
  }

  test("That mergeDeletableLanguageFields works as expected") {
    val existing = Seq(domain.ImageTitle("Hei", "nb"), domain.ImageTitle("Hå", "nn"), domain.ImageTitle("Ho", "en"))

    writeService.mergeDeletableLanguageFields[domain.ImageTitle](
      existing,
      Right(Some(domain.ImageTitle("Yop", "nb"))),
      "nb",
    ) should be(Seq(domain.ImageTitle("Hå", "nn"), domain.ImageTitle("Ho", "en"), domain.ImageTitle("Yop", "nb")))

    writeService.mergeDeletableLanguageFields(existing, Right(None), "nb") should be(existing)

    writeService.mergeDeletableLanguageFields(existing, Left(null), "nb") should be(
      Seq(domain.ImageTitle("Hå", "nn"), domain.ImageTitle("Ho", "en"))
    )

  }

  test("That uploading valid JPEG should generate image variants of multiple sizes") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any, any, any, any)).thenAnswer(i => Success(i.getArgument(0)))
    val expectedDimensions   = domain.ImageDimensions(640, 426)
    val expectedVariantSizes = ImageVariantSize.forDimensions(expectedDimensions)
    expectedVariantSizes.size should be > 0

    val domain.UploadedImage(_, _, _, dimensions, variants) = writeService
      .uploadImageWithVariants(TestData.childrensImageUploadedFile)
      .failIfFailure

    // Should be called once for original image + once for each variant
    verify(imageStorage, times(expectedVariantSizes.size + 1)).uploadFromStream(any, any, any, any)
    dimensions should equal(Some(expectedDimensions))
    variants.size should equal(expectedVariantSizes.size)
    variants.foreach { variant =>
      variant.bucketKey should endWith(s"/${variant.size.entryName}.webp")
    }
  }
}
