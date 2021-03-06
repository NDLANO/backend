/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.api._
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.domain.{ImageFileDataDocument, ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
import no.ndla.scalatra.error.ValidationException
import org.mockito.invocation.InvocationOnMock
import org.scalatra.servlet.FileItem
import scalikejdbc.DBSession

import java.time.LocalDateTime
import java.io.InputStream
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val writeService     = new WriteService
  override val converterService = new ConverterService
  val newFileName               = "AbCdeF.mp3"
  val fileMock1: FileItem       = mock[FileItem]

  val newImageMeta = NewImageMetaInformationV2(
    "title",
    "alt text",
    Copyright(License("by", "", None), "", Seq.empty, Seq.empty, Seq.empty, None, None, None),
    Seq.empty,
    "",
    "en",
    Some(ModelReleasedStatus.YES.toString)
  )

  def updated() = LocalDateTime.of(2017, 4, 1, 12, 15, 32)

  val domainImageMeta =
    converterService
      .asDomainImageMetaInformationV2(newImageMeta)
      .get

  val multiLangImage = new ImageMetaInformation(
    id = Some(2),
    titles =
      List(domain.ImageTitle("nynorsk", "nn"), domain.ImageTitle("english", "en"), domain.ImageTitle("norsk", "und")),
    alttexts = List(),
    images = Seq(
      new domain.ImageFileData(
        id = 65123,
        fileName = "yolo.jpeg",
        size = 100,
        contentType = "image/jpeg",
        dimensions = None,
        language = "nb",
        imageMetaId = 2
      )
    ),
    copyright = domain.Copyright("", "", List(), List(), List(), None, None, None),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  override def beforeEach(): Unit = {
    when(fileMock1.getContentType).thenReturn(Some("image/jpeg"))
    val bytes = TestData.NdlaLogoImage.stream.readAllBytes()
    when(fileMock1.get()).thenReturn(bytes)
    when(fileMock1.size).thenReturn(1024)
    when(fileMock1.name).thenReturn("file.jpg")
    when(random.string(any)).thenCallRealMethod()

    val applicationUrl = mock[HttpServletRequest]
    when(applicationUrl.getHeader(any[String])).thenReturn("http")
    when(applicationUrl.getServerName).thenReturn("localhost")
    when(applicationUrl.getServletPath).thenReturn("/image-api/v2/images/")
    ApplicationUrl.set(applicationUrl)

    reset(imageRepository, imageIndexService, imageStorage, tagIndexService)
    when(imageRepository.insert(any[ImageMetaInformation])(any[DBSession]))
      .thenReturn(domainImageMeta.copy(id = Some(1)))
  }

  test("randomFileName should return a random filename with a given length and extension") {
    val extension = ".jpg"

    val result = writeService.randomFileName(extension)
    result.length should be(12)
    result.endsWith(extension) should be(true)

    val resultWithNegativeLength = writeService.randomFileName(extension, -1)
    resultWithNegativeLength.length should be(1 + extension.length)
    resultWithNegativeLength.endsWith(extension) should be(true)
  }

  test("randomFileName should return an extensionless filename if empty extension is supplied") {
    val result = writeService.randomFileName("")
    result.length should be(12)
    result.contains(".") should be(false)
  }

  test("uploadFile should return Success if file upload succeeds") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    val expectedImage =
      domain.ImageFileData(1, newFileName, 1024, "image/jpeg", Some(domain.ImageDimensions(189, 60)), "nb", 54)

    when(imageRepository.insertImageFile(any, any, any)(any)).thenReturn(Success(expectedImage))
    val result = writeService.uploadImage(1, fileMock1, "nb")
    verify(imageStorage, times(1)).uploadFromStream(any[InputStream], any[String], any[String], any[Long])

    result should equal(
      Success(expectedImage)
    )
  }

  test("uploadFile should return Failure if file upload failed") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Failure(new RuntimeException))

    writeService.uploadImage(1, fileMock1, "nb").isFailure should be(true)
  }

  test("storeNewImage should return Failure if upload failes") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Failure(new RuntimeException))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
  }

  test("storeNewImage should return Failure if validation fails") {
    reset(imageRepository, imageIndexService, imageStorage)
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None)))
      .thenReturn(Failure(new ValidationException(errors = Seq())))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageRepository, times(0)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageIndexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(0)).cloneObject(any, any)
    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
  }

  test("storeNewImage should return Failure if failed to insert into database") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageRepository.insert(any[ImageMetaInformation])(any[DBSession])).thenThrow(new RuntimeException)
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageIndexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(0)).cloneObject(any, any)
    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
  }

  test("storeNewImage should return Failure if failed to index image metadata") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))
    when(imageRepository.insert(any)(any)).thenReturn(domainImageMeta.copy(id = Some(100)))
    when(imageRepository.insertImageFile(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      val imageId  = i.getArgument[Long](0)
      val fileName = i.getArgument[String](1)
      val document = i.getArgument[ImageFileDataDocument](2)
      Success(document.toFull(1, fileName, imageId))
    })

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to index tag metadata") {
    val afterInsert = domainImageMeta.copy(id = Some(1))
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(imageStorage.deleteObject(any)).thenReturn(Success(()))
    when(imageRepository.insert(any)(any)).thenReturn(afterInsert)
    when(imageRepository.insertImageFile(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      val imageId  = i.getArgument[Long](0)
      val fileName = i.getArgument[String](1)
      val document = i.getArgument[ImageFileDataDocument](2)
      Success(document.toFull(1, fileName, imageId))
    })

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
    verify(imageIndexService, times(1)).deleteDocument(eqTo(1))
  }

  test("storeNewImage should return Success if creation of new image file succeeded") {
    val afterInsert = domainImageMeta.copy(id = Some(1))
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(imageRepository.insert(any)(any)).thenReturn(afterInsert)
    var fullImage: Option[domain.ImageFileData] = None
    when(imageRepository.insertImageFile(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      val imageId  = i.getArgument[Long](0)
      val fileName = i.getArgument[String](1)
      val document = i.getArgument[ImageFileDataDocument](2)
      fullImage = Some(document.toFull(1, fileName, imageId))
      Success(fullImage.get)
    })

    val result = writeService.storeNewImage(newImageMeta, fileMock1)
    result.isSuccess should be(true)
    result should equal(Success(afterInsert.copy(images = Seq(fullImage.get))))

    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
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
    when(authUser.userOrClientid()).thenReturn("ndla54321")
    when(clock.now()).thenReturn(updated())
    val domain =
      converterService
        .asDomainImageMetaInformationV2(newImageMeta)
        .get
    domain.updatedBy should equal("ndla54321")
    domain.updated should equal(updated())
  }

  test("mergeImages should append a new language if language not already exists") {
    val date     = LocalDateTime.now()
    val user     = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformation(
      "en",
      Some("Title"),
      Some("AltText"),
      None,
      None,
      None,
      None
    )

    val expectedResult = existing.copy(
      titles = List(existing.titles.head, domain.ImageTitle("Title", "en")),
      alttexts = List(existing.alttexts.head, domain.ImageAltText("AltText", "en")),
      editorNotes = Seq(domain.EditorNote(date, user, "Added new language 'en'."))
    )

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("mergeImages overwrite a languages if specified language already exist in cover") {
    val date     = LocalDateTime.now()
    val user     = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformation(
      "nb",
      Some("Title"),
      Some("AltText"),
      None,
      None,
      None,
      None
    )

    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      editorNotes = Seq(domain.EditorNote(date, user, "Updated image data."))
    )

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("mergeImages updates optional values if specified") {
    val date     = LocalDateTime.now()
    val user     = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformation(
      "nb",
      Some("Title"),
      Some("AltText"),
      Some(
        Copyright(
          License("testLic", "License for testing", None),
          "test",
          List(Author("Opphavsmann", "Testerud")),
          List(),
          List(),
          None,
          None,
          None
        )
      ),
      Some(List("a", "b", "c")),
      Some("Caption"),
      Some(ModelReleasedStatus.NO.toString)
    )

    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      copyright = domain
        .Copyright("testLic", "test", List(domain.Author("Opphavsmann", "Testerud")), List(), List(), None, None, None),
      tags = List(domain.ImageTag(List("a", "b", "c"), "nb")),
      captions = List(domain.ImageCaption("Caption", "nb")),
      modelReleased = ModelReleasedStatus.NO,
      editorNotes = Seq(domain.EditorNote(date, "ndla124", "Updated image data."))
    )

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("that deleting image deletes database entry, s3 object, and indexed document") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val imageId = 4444.toLong
    val domainWithImage = domainImageMeta.copy(
      images = Seq(
        domain.ImageFileData(1, newFileName, 1024, "image/jpeg", Some(domain.ImageDimensions(189, 60)), "nb", 54)
      )
    )

    when(imageRepository.withId(imageId)).thenReturn(Some(domainWithImage))
    when(imageRepository.delete(eqTo(imageId))(any[DBSession])).thenReturn(1)
    when(imageStorage.deleteObject(any[String])).thenReturn(Success(()))
    when(imageIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Long](0))
    )
    when(tagIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Long](0)))

    writeService.deleteImageAndFiles(imageId)

    verify(imageStorage, times(1)).deleteObject(domainWithImage.images.head.fileName)
    verify(imageIndexService, times(1)).deleteDocument(imageId)
    verify(tagIndexService, times(1)).deleteDocument(imageId)
    verify(imageRepository, times(1)).delete(eqTo(imageId))(any[DBSession])
  }

  test("That deleting language version deletes language") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val date = LocalDateTime.now()
    val user = "ndla124"

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    val imageId = 5555.toLong
    val image   = multiLangImage.copy(id = Some(imageId))
    val expectedImage =
      image.copy(
        titles = List(domain.ImageTitle("english", "en"), domain.ImageTitle("norsk", "und")),
        editorNotes = image.editorNotes :+ domain.EditorNote(date, user, "Deleted language 'nn'.")
      )

    when(imageRepository.withId(imageId)).thenReturn(Some(image))
    when(imageRepository.update(any[ImageMetaInformation], eqTo(imageId))(any)).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[ImageMetaInformation](0)
    )
    when(validationService.validate(any[ImageMetaInformation], any[Option[ImageMetaInformation]]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[ImageMetaInformation](0)))
    when(imageIndexService.indexDocument(any[ImageMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[ImageMetaInformation](0)))
    when(tagIndexService.indexDocument(any[ImageMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[ImageMetaInformation](0)))

    writeService.deleteImageLanguageVersionV2(imageId, "nn")

    verify(imageRepository, times(1)).update(eqTo(expectedImage), eqTo(imageId))(any)
  }

  test("That deleting last language version deletes entire image") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val imageId = 6666.toLong
    val image = multiLangImage.copy(
      id = Some(imageId),
      titles = List(domain.ImageTitle("english", "en")),
      captions = List(domain.ImageCaption("english", "en")),
      tags = Seq(domain.ImageTag(Seq("eng", "elsk"), "en")),
      alttexts = Seq(domain.ImageAltText("english", "en")),
      images = Seq(TestData.bjorn.images.head.copy(language = "en"))
    )

    when(imageRepository.withId(imageId)).thenReturn(Some(image))
    when(imageRepository.delete(eqTo(imageId))(any[DBSession])).thenReturn(1)
    when(imageStorage.deleteObject(any[String])).thenReturn(Success(()))
    when(imageIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Long](0))
    )
    when(tagIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Long](0)))

    writeService.deleteImageLanguageVersionV2(imageId, "en")

    verify(imageStorage, times(1)).deleteObject(image.images.head.fileName)
    verify(imageIndexService, times(1)).deleteDocument(imageId)
    verify(tagIndexService, times(1)).deleteDocument(imageId)
    verify(imageRepository, times(1)).delete(eqTo(imageId))(any[DBSession])
  }

  test("That updating image file with multiple same filepaths does not override filepath") {
    reset(validationService, imageRepository, imageStorage)
    val imageId  = 100
    val coolDate = LocalDateTime.now()
    val upd = UpdateImageMetaInformation(
      language = "nb",
      title = Some("new title"),
      alttext = None,
      copyright = None,
      tags = None,
      caption = None,
      modelReleased = None
    )
    val image = domain.ImageFileData(
      id = 1,
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpg",
      dimensions = None,
      language = "nb",
      imageMetaId = imageId
    )

    val dbImage = TestData.bjorn.copy(
      images = Seq(
        image.copy(id = 1, language = "nn"),
        image.copy(id = 2, language = "nb")
      ),
      updated = coolDate,
      updatedBy = "ndla124"
    )

    val fileMock = mock[FileItem]
    when(fileMock.name).thenReturn("someupload.jpg")
    when(fileMock.getContentType).thenReturn(Some("image/jpg"))
    when(fileMock.get()).thenReturn(TestData.NdlaLogoImage.stream.readAllBytes())
    when(fileMock.size).thenReturn(1337)
    when(authUser.userOrClientid()).thenReturn("ndla124")
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Some(dbImage))
    when(imageRepository.update(any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      i.getArgument[domain.ImageMetaInformation](0)
    })
    when(imageStorage.cloneObject(any, any)).thenReturn(Success(()))
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

    when(imageRepository.insertImageFile(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      val imageId  = i.getArgument[Long](0)
      val fileName = i.getArgument[String](1)
      val doc      = i.getArgument[domain.ImageFileDataDocument](2)
      val image    = doc.toFull(5, fileName, imageId)
      Success(image)
    })

    val expectedResult =
      dbImage.copy(
        titles = Seq(domain.ImageTitle("new title", "nb")),
        images = Seq(
          image.copy(id = 1, language = "nn"),
          image.copy(
            id = 5,
            fileName = "randomstring.jpg",
            size = 1337,
            dimensions = Some(domain.ImageDimensions(189, 60)),
            language = "nb"
          )
        ),
        editorNotes = List(
          domain.EditorNote(coolDate, "ndla124", "Updated image file for 'nb' language."),
          domain.EditorNote(coolDate, "ndla124", "Updated image data.")
        )
      )

    val result = writeService.updateImageAndFile(imageId, upd, Some(fileMock))
    result should be(Success(expectedResult))

    verify(imageStorage, times(1)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
    verify(imageStorage, times(0)).cloneObject(any, any)
    verify(imageRepository, times(1)).update(any, any)(any)
    verify(imageRepository, times(1)).insertImageFile(any, any, any)(any)
  }

  test("That uploading image for a new language just adds a new one") {
    reset(validationService, imageRepository, imageStorage)
    val imageId  = 100
    val coolDate = LocalDateTime.now()
    val upd = UpdateImageMetaInformation(
      language = "nb",
      title = Some("new title"),
      alttext = None,
      copyright = None,
      tags = None,
      caption = None,
      modelReleased = None
    )
    val image = domain.ImageFileData(
      id = 1,
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpg",
      dimensions = None,
      language = "nb",
      imageMetaId = imageId
    )

    val dbImage = TestData.bjorn.copy(
      images = Seq(
        image.copy(id = 1, language = "nn")
      ),
      updated = coolDate,
      updatedBy = "ndla124"
    )

    val fileMock = mock[FileItem]
    when(fileMock.name).thenReturn("someupload.jpg")
    when(fileMock.getContentType).thenReturn(Some("image/jpg"))
    when(fileMock.get()).thenReturn(TestData.NdlaLogoImage.stream.readAllBytes())
    when(fileMock.size).thenReturn(1337)
    when(authUser.userOrClientid()).thenReturn("ndla124")
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Some(dbImage))
    when(imageRepository.update(any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      i.getArgument[domain.ImageMetaInformation](0)
    })
    when(imageStorage.cloneObject(any, any)).thenReturn(Success(()))
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

    when(imageRepository.insertImageFile(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      val imageId  = i.getArgument[Long](0)
      val fileName = i.getArgument[String](1)
      val doc      = i.getArgument[domain.ImageFileDataDocument](2)
      val image    = doc.toFull(5, fileName, imageId)
      Success(image)
    })

    val expectedResult =
      dbImage.copy(
        titles = Seq(domain.ImageTitle("new title", "nb")),
        images = Seq(
          image.copy(id = 1, language = "nn"),
          image.copy(
            id = 5,
            fileName = "randomstring.jpg",
            size = 1337,
            dimensions = Some(domain.ImageDimensions(189, 60)),
            language = "nb"
          )
        ),
        editorNotes = List(
          domain.EditorNote(coolDate, "ndla124", "Updated image file for 'nb' language."),
          domain.EditorNote(coolDate, "ndla124", "Updated image data.")
        )
      )

    val result = writeService.updateImageAndFile(imageId, upd, Some(fileMock))
    result should be(Success(expectedResult))

    verify(imageStorage, times(1)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
    verify(imageStorage, times(0)).cloneObject(any, any)
    verify(imageRepository, times(1)).update(any, any)(any)
    verify(imageRepository, times(1)).insertImageFile(any, any, any)(any)
  }

  test("Deleting language version should delete file if only used by that language") {
    reset(validationService, imageRepository, imageStorage)
    val imageId  = 100
    val coolDate = LocalDateTime.now()

    val image = domain.ImageFileData(
      id = 1,
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpg",
      dimensions = None,
      language = "nb",
      imageMetaId = imageId
    )

    val dbImage = TestData.bjorn.copy(
      titles = Seq(
        domain.ImageTitle("hei nb", "nb"),
        domain.ImageTitle("hei nn", "nn")
      ),
      images = Seq(
        image.copy(id = 1, fileName = "hello-nb.jpg", language = "nb"),
        image.copy(id = 2, fileName = "hello-nn.jpg", language = "nn")
      ),
      updated = coolDate,
      updatedBy = "ndla124"
    )

    when(authUser.userOrClientid()).thenReturn("ndla124")
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Some(dbImage))
    when(imageRepository.update(any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      i.getArgument[domain.ImageMetaInformation](0)
    })
    when(imageStorage.cloneObject(any, any)).thenReturn(Success(()))
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

    val expectedResult =
      dbImage.copy(
        titles = Seq(domain.ImageTitle("hei nb", "nb")),
        images = Seq(image.copy(id = 1, fileName = "hello-nb.jpg", language = "nb")),
        editorNotes = Seq(domain.EditorNote(coolDate, "ndla124", "Deleted language 'nn'."))
      )

    val result = writeService.deleteImageLanguageVersion(imageId, "nn")
    result should be(Success(Some(expectedResult)))

    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(1)).deleteObject(eqTo("hello-nn.jpg"))
    verify(imageStorage, times(0)).cloneObject(any, any)
    verify(imageRepository, times(1)).update(any, any)(any)
    verify(imageRepository, times(0)).insertImageFile(any, any, any)(any)
  }

  test("Deleting language version should not delete file if it used by more languages") {
    reset(validationService, imageRepository, imageStorage)
    val imageId  = 100
    val coolDate = LocalDateTime.now()

    val image = domain.ImageFileData(
      id = 1,
      fileName = "apekatt.jpg",
      size = 100,
      contentType = "image/jpg",
      dimensions = None,
      language = "nb",
      imageMetaId = imageId
    )

    val dbImage = TestData.bjorn.copy(
      titles = Seq(
        domain.ImageTitle("hei nb", "nb"),
        domain.ImageTitle("hei nn", "nn")
      ),
      images = Seq(
        image.copy(id = 1, fileName = "hello-shared.jpg", language = "nb"),
        image.copy(id = 2, fileName = "hello-shared.jpg", language = "nn")
      ),
      updated = coolDate,
      updatedBy = "ndla124"
    )

    when(authUser.userOrClientid()).thenReturn("ndla124")
    when(validationService.validateImageFile(any)).thenReturn(None)
    when(validationService.validate(any, any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[domain.ImageMetaInformation](0))
    })
    when(imageRepository.withId(imageId)).thenReturn(Some(dbImage))
    when(imageRepository.update(any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      i.getArgument[domain.ImageMetaInformation](0)
    })
    when(imageStorage.cloneObject(any, any)).thenReturn(Success(()))
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

    val expectedResult =
      dbImage.copy(
        titles = Seq(domain.ImageTitle("hei nb", "nb")),
        images = Seq(image.copy(id = 1, fileName = "hello-shared.jpg", language = "nb")),
        editorNotes = Seq(domain.EditorNote(coolDate, "ndla124", "Deleted language 'nn'."))
      )

    val result = writeService.deleteImageLanguageVersion(imageId, "nn")
    result should be(Success(Some(expectedResult)))

    verify(imageStorage, times(0)).uploadFromStream(any, any, any, any)
    verify(imageStorage, times(0)).deleteObject(any)
    verify(imageStorage, times(0)).cloneObject(any, any)
    verify(imageRepository, times(1)).update(any, any)(any)
    verify(imageRepository, times(0)).insertImageFile(any, any, any)(any)
  }
}
