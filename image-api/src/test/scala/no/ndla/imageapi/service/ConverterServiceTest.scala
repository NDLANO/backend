/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Copyright
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService

  val updated: NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)

  val someDims: Some[ImageDimensions] = Some(ImageDimensions(100, 100))
  val full                            = new ImageFileData(1, "/123.png", 200, "image/png", someDims, "nb", 5)
  val wanting                         = new ImageFileData(2, "123.png", 200, "image/png", someDims, "und", 6)

  val DefaultImageMetaInformation = new ImageMetaInformation(
    id = Some(1),
    titles = List(ImageTitle("test", "nb")),
    alttexts = List(),
    images = Some(Seq(full)),
    copyright = Copyright("", None, List(), List(), List(), None, None, false),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  val WantingImageMetaInformation = new ImageMetaInformation(
    id = Some(1),
    titles = List(ImageTitle("test", "nb")),
    alttexts = List(),
    images = Some(Seq(wanting)),
    copyright = Copyright("", None, List(), List(), List(), None, None, false),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  val MultiLangImage = new ImageMetaInformation(
    id = Some(2),
    titles = List(ImageTitle("nynorsk", "nn"), ImageTitle("english", "en"), ImageTitle("norsk", "und")),
    alttexts = List(),
    images = Some(Seq(full.copy(language = "nn"))),
    copyright = Copyright("", None, List(), List(), List(), None, None, false),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  test("That asApiImageMetaInformationWithDomainUrl returns links with domain urls") {
    {
      val Success(apiImage) =
        converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("nb"), None)
      apiImage.metaUrl should equal(s"${props.ImageApiV2UrlBase}1")
      apiImage.imageUrl should equal(s"${props.RawImageUrlBase}/123.png")
    }
    {
      val Success(apiImage) =
        converterService.asApiImageMetaInformationWithDomainUrlV2(WantingImageMetaInformation, Some("nb"), None)
      apiImage.metaUrl should equal(s"${props.ImageApiV2UrlBase}1")
      apiImage.imageUrl should equal(s"${props.RawImageUrlBase}/123.png")
    }
  }

  test("That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links with applicationUrl") {
    val Success(apiImage) =
      converterService.asApiImageMetaInformationWithApplicationUrlV2(DefaultImageMetaInformation, None, None)
    apiImage.metaUrl should equal(s"${props.Domain}/image-api/v2/images/1")
    apiImage.imageUrl should equal(s"${props.Domain}/image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links with domain urls") {
    val Success(apiImage) =
      converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, None, None)
    apiImage.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test(
    "That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links even if language is not supported"
  ) {
    val Success(apiImage) = converterService.asApiImageMetaInformationWithApplicationUrlV2(
      DefaultImageMetaInformation,
      Some("RandomLangauge"),
      None
    )

    apiImage.metaUrl should equal(s"${props.Domain}/image-api/v2/images/1")
    apiImage.imageUrl should equal(s"${props.Domain}/image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links even if language is not supported") {
    val Success(apiImage) =
      converterService.asApiImageMetaInformationWithDomainUrlV2(
        DefaultImageMetaInformation,
        Some("RandomLangauge"),
        None
      )
    apiImage.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test("that asImageMetaInformationV2 properly") {
    val Success(result1) = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None, None)
    result1.id should be("2")
    result1.title.language should be("nn")

    val Success(result2) = converterService.asImageMetaInformationV2(MultiLangImage, Some("en"), "", None, None)
    result2.id should be("2")
    result2.title.language should be("en")

    val Success(result3) = converterService.asImageMetaInformationV2(MultiLangImage, Some("nn"), "", None, None)
    result3.id should be("2")
    result3.title.language should be("nn")

  }

  test("that asImageMetaInformationV2 returns sorted supportedLanguages") {
    val Success(result) = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None, None)
    result.supportedLanguages should be(Seq("nn", "en", "und"))
  }

  test("that withoutLanguage removes correct language") {
    val result1 = converterService.withoutLanguage(MultiLangImage, "en", TokenUser.SystemUser)
    converterService.getSupportedLanguages(result1) should be(Seq("nn", "und"))
    val result2 = converterService.withoutLanguage(MultiLangImage, "nn", TokenUser.SystemUser)
    converterService.getSupportedLanguages(result2) should be(Seq("en", "und"))
    val result3 = converterService.withoutLanguage(MultiLangImage, "und", TokenUser.SystemUser)
    converterService.getSupportedLanguages(result3) should be(Seq("nn", "en"))
    val result4 = converterService.withoutLanguage(
      converterService.withoutLanguage(MultiLangImage, "und", TokenUser.SystemUser),
      "en",
      TokenUser.SystemUser
    )
    converterService.getSupportedLanguages(result4) should be(Seq("nn"))
  }

  test("That with new image returns metadata from new image") {
    val newImage = new ImageFileData(
      id = 1,
      fileName = "somename.jpg",
      size = 123,
      contentType = "image/jpg",
      dimensions = Some(ImageDimensions(123, 555)),
      language = "nb",
      imageMetaId = 4
    )

    val result = converterService.withNewImage(MultiLangImage, newImage, "nb", TokenUser.SystemUser)
    result.images.get.find(_.language == "nb").get.size should be(123)
    result.images.get.find(_.language == "nb").get.dimensions should be(Some(ImageDimensions(123, 555)))
    result.images.get.find(_.language == "nb").get.contentType should be("image/jpg")
  }

}
