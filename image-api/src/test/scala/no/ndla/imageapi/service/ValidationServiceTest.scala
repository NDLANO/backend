/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, Tag, UploadedFile}
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License.CC_BY
import org.mockito.Mockito.{reset, when}

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService = new ValidationService

  val fileMock: UploadedFile = mock[UploadedFile]
  def updated(): NDLADate    = NDLADate.of(2017, 4, 1, 12, 15, 32)

  val sampleImageMeta = new ImageMetaInformation(
    id = Some(1),
    titles = Seq.empty,
    alttexts = Seq.empty,
    images = Some(
      Seq(
        new ImageFileData(
          id = 1,
          fileName = "image.jpg",
          size = 1024,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          imageMetaId = 1
        )
      )
    ),
    copyright =
      Copyright(CC_BY.toString, None, Seq(Author("originator", "test")), Seq.empty, Seq.empty, None, None, false),
    tags = Seq.empty,
    captions = Seq.empty,
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  override def beforeEach(): Unit = {
    reset(fileMock)
  }

  test("validateImageFile returns a validation message if file has an unknown extension") {
    val fileName = "image.asdf"
    when(fileMock.fileName).thenReturn(Some(fileName))
    val Some(result) = validationService.validateImageFile(fileMock)

    result.message.contains(s"The file $fileName does not have a known file extension") should be(true)
  }

  test("validateImageFile returns a validation message if content type is unknown") {
    val fileName = "image.jpg"
    when(fileMock.fileName).thenReturn(Some(fileName))
    when(fileMock.contentType).thenReturn(Some("text/html"))
    val Some(result) = validationService.validateImageFile(fileMock)

    result.message.contains(s"The file $fileName is not a valid image file.") should be(true)
  }

  test("validateImageFile returns None if image file is valid") {
    val fileName = "image.jpg"
    when(fileMock.fileName).thenReturn(Some(fileName))
    when(fileMock.contentType).thenReturn(Some("image/jpeg"))
    validationService.validateImageFile(fileMock).isDefined should be(false)
  }

  test("validate returns a validation error if title contains html") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("<h1>title</h1>", "nb")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)
    exception.errors.head.message.contains("contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if title language is invalid") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("title", "invalid")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if title is valid") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("title", "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns a validation error if copyright contains an invalid license") {
    val imageMeta =
      sampleImageMeta.copy(
        copyright =
          Copyright("invalid", None, Seq(Author("originator", "test")), Seq.empty, Seq.empty, None, None, false)
      )
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("invalid is not a valid license") should be(true)
  }

  test("validate returns a validation error if copyright origin contains html") {
    val imageMeta = sampleImageMeta.copy(
      copyright = Copyright(
        CC_BY.toString,
        Some("<h1>origin</h1>"),
        Seq(Author("originator", "test")),
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    )
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if author contains html") {
    val imageMeta = sampleImageMeta.copy(
      copyright = Copyright(
        CC_BY.toString,
        None,
        Seq(Author("originator", "<h1>Drumpf</h1>")),
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    )
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns success if copyright is valid") {
    val imageMeta = sampleImageMeta.copy(
      copyright = Copyright(
        CC_BY.toString,
        Some("ntb"),
        Seq(Author("originator", "Drumpf")),
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    )
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns error if authortype is invalid") {
    val imageMeta = sampleImageMeta.copy(
      copyright = Copyright(
        CC_BY.toString,
        Some("ntb"),
        Seq(Author("invalidType", "Drumpf")),
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    )
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains(
      "Author is of illegal type. Must be one of originator, photographer, artist, writer, scriptwriter, reader, translator, director, illustrator, cowriter, composer"
    ) should be(true)
  }

  test("validate returns a validation error if tags contain html") {
    val imageMeta = sampleImageMeta.copy(tags = Seq(Tag(Seq("<h1>tag</h1>"), "en")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if tags language is invalid") {
    val imageMeta = sampleImageMeta.copy(tags = Seq(Tag(Seq("tag"), "invalid")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if tags are valid") {
    val imageMeta = sampleImageMeta.copy(tags = Seq(Tag(Seq("tag"), "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns a validation error if alt texts contain html") {
    val imageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("<h1>alt text</h1>", "en")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if alt texts language is invalid") {
    val imageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("alt text", "invalid")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if alt texts are valid") {
    val imageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("alt text", "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns a validation error if captions contain html") {
    val imageMeta = sampleImageMeta.copy(captions = Seq(ImageCaption("<h1>caption</h1>", "en")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if captions language is invalid") {
    val imageMeta = sampleImageMeta.copy(captions = Seq(ImageCaption("caption", "invalid")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if captions are valid") {
    val imageMeta = sampleImageMeta.copy(captions = Seq(ImageCaption("caption", "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns error with invalid language if it does not already exist") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("new image title", "xyz")))
    val result    = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)
    exception.errors.head.message.contains("Language 'xyz' is not a supported value.") should be(true)
    result.isSuccess should be(false)
  }

  test("validate returns success with unknown language if it already exist") {
    val imageMeta    = sampleImageMeta.copy(titles = Seq(ImageTitle("new image title", "und")))
    val oldImageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("old image title", "und")))
    val result       = validationService.validate(imageMeta, Some(oldImageMeta))
    result.isSuccess should be(true)
  }

  test("validate returns success with unknown language if it already exist, also in another field") {
    val imageMeta    = sampleImageMeta.copy(titles = Seq(ImageTitle("new image title", "und")))
    val oldImageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("new image alttext", "und")))
    val result       = validationService.validate(imageMeta, Some(oldImageMeta))
    result.isSuccess should be(true)
  }

  test("validateCopyright fails when no copyright holders are provided") {
    val copyright  = sampleImageMeta.copyright.copy(processors = Seq(), creators = Seq(), rightsholders = Seq())
    val exceptions = validationService.validateCopyright(copyright)
    exceptions.length should be(1)
    exceptions should be(
      Seq(
        ValidationMessage(
          "license.license",
          s"At least one copyright holder is required when license is ${CC_BY.toString}"
        )
      )
    )
  }

  test("validateCopyright succeeds when at least one copyright holder is provided") {
    val copyright = sampleImageMeta.copyright
    val result    = validationService.validateCopyright(copyright)
    print(result)
    result.length should be(0)
  }

}
