/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
import org.joda.time.{DateTime, DateTimeZone}

import java.util.Date
import javax.servlet.http.HttpServletRequest
import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService

  val updated: Date = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val someDims = Some(ImageDimensions(100, 100))
  val full     = new Image(1, "/123.png", 200, "image/png", someDims, "nb", 5)
  val wanting  = new Image(2, "123.png", 200, "image/png", someDims, "und", 6)

  val DefaultImageMetaInformation = new ImageMetaInformation(
    id = Some(1),
    titles = List(ImageTitle("test", "nb")),
    alttexts = List(),
    images = Seq(full),
    copyright = Copyright("", "", List(), List(), List(), None, None, None),
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
    images = Seq(wanting),
    copyright = Copyright("", "", List(), List(), List(), None, None, None),
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
    images = Seq(full),
    copyright = Copyright("", "", List(), List(), List(), None, None, None),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  override def beforeEach(): Unit = {
    val request = mock[HttpServletRequest]
    when(request.getServerPort).thenReturn(80)
    when(request.getScheme).thenReturn("http")
    when(request.getServerName).thenReturn("image-api")
    when(request.getServletPath).thenReturn("/v2/images")

    ApplicationUrl.set(request)
  }

  override def afterEach(): Unit = {
    ApplicationUrl.clear()
  }

  test("That asApiImageMetaInformationWithDomainUrl returns links with domain urls") {
    {
      val Success(apiImage) =
        converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("nb"))
      apiImage.metaUrl should equal(s"${props.ImageApiUrlBase}1")
      apiImage.imageUrl should equal(s"${props.RawImageUrlBase}/123.png")
    }
    {
      val Success(apiImage) =
        converterService.asApiImageMetaInformationWithDomainUrlV2(WantingImageMetaInformation, Some("nb"))
      apiImage.metaUrl should equal(s"${props.ImageApiUrlBase}1")
      apiImage.imageUrl should equal(s"${props.RawImageUrlBase}/123.png")
    }
  }

  test("That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links with applicationUrl") {
    val Success(apiImage) =
      converterService.asApiImageMetaInformationWithApplicationUrlV2(DefaultImageMetaInformation, None)
    apiImage.metaUrl should equal(s"${props.Domain}/v2/images/1")
    apiImage.imageUrl should equal(s"${props.Domain}/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links with domain urls") {
    val Success(apiImage) = converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, None)
    apiImage.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test(
    "That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links even if language is not supported"
  ) {
    val Success(apiImage) = converterService.asApiImageMetaInformationWithApplicationUrlV2(
      DefaultImageMetaInformation,
      Some("RandomLangauge")
    )

    apiImage.metaUrl should equal(s"${props.Domain}/v2/images/1")
    apiImage.imageUrl should equal(s"${props.Domain}/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links even if language is not supported") {
    val Success(apiImage) =
      converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("RandomLangauge"))
    apiImage.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithApplicationUrlV2 returns with agreement copyright features") {
    val from = DateTime.now().minusDays(5).toDate()
    val to   = DateTime.now().plusDays(10).toDate()
    val agreementCopyright = api.Copyright(
      api.License("gnu", "gpl", None),
      "http://tjohei.com/",
      List(),
      List(),
      List(api.Author("Supplier", "Mads LakseService")),
      None,
      Some(from),
      Some(to)
    )
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreementCopyright))
    val Success(apiImage) = converterService.asApiImageMetaInformationWithApplicationUrlV2(
      DefaultImageMetaInformation.copy(
        copyright = DefaultImageMetaInformation.copyright.copy(
          processors = List(Author("Idea", "Kaptein Snabelfant")),
          rightsholders = List(Author("Publisher", "KjeksOgKakerAS")),
          agreementId = Some(1)
        )
      ),
      None
    )

    apiImage.copyright.creators.size should equal(0)
    apiImage.copyright.processors.head.name should equal("Kaptein Snabelfant")
    apiImage.copyright.rightsholders.head.name should equal("Mads LakseService")
    apiImage.copyright.rightsholders.size should equal(1)
    apiImage.copyright.license.license should equal("gnu")
    apiImage.copyright.validFrom.get should equal(from)
    apiImage.copyright.validTo.get should equal(to)
  }

  test("that asImageMetaInformationV2 properly") {
    val Success(result1) = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None)
    result1.id should be("2")
    result1.title.language should be("nn")

    val Success(result2) = converterService.asImageMetaInformationV2(MultiLangImage, Some("en"), "", None)
    result2.id should be("2")
    result2.title.language should be("en")

    val Success(result3) = converterService.asImageMetaInformationV2(MultiLangImage, Some("nn"), "", None)
    result3.id should be("2")
    result3.title.language should be("nn")

  }

  test("that asImageMetaInformationV2 returns sorted supportedLanguages") {
    val Success(result) = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None)
    result.supportedLanguages should be(Seq("nn", "en", "und"))
  }

  test("that withoutLanguage removes correct language") {
    val result1 = converterService.withoutLanguage(MultiLangImage, "en")
    converterService.getSupportedLanguages(result1) should be(Seq("nn", "und"))
    val result2 = converterService.withoutLanguage(MultiLangImage, "nn")
    converterService.getSupportedLanguages(result2) should be(Seq("en", "und"))
    val result3 = converterService.withoutLanguage(MultiLangImage, "und")
    converterService.getSupportedLanguages(result3) should be(Seq("nn", "en"))
    val result4 = converterService.withoutLanguage(converterService.withoutLanguage(MultiLangImage, "und"), "en")
    converterService.getSupportedLanguages(result4) should be(Seq("nn"))
  }

  test("That with new image returns metadata from new image") {
    val newImage = new Image(
      id = 1,
      fileName = "somename.jpg",
      size = 123,
      contentType = "image/jpg",
      dimensions = Some(ImageDimensions(123, 555)),
      language = "nb",
      imageMetaId = 4
    )

    val result = converterService.withNewImage(MultiLangImage, newImage, "nb")
    result.images.head.size should be(123)
    result.images.head.dimensions should be(Some(ImageDimensions(123, 555)))
    result.images.head.contentType should be("image/jpg")
  }

}
