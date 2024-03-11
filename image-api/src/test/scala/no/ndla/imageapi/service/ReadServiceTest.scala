/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.imageapi.model.api.ImageMetaInformationV2
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.model.{InvalidUrlException, api, domain}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.json4s.*
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonParser
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doReturn, reset, times, verify, when}
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService      = new ReadService
  override val converterService = new ConverterService

  test("That path to id conversion works as expected for id paths") {
    val id                = 1234L
    val imageUrl          = "apekatt.jpg"
    val expectedImageFile = TestData.bjorn.images.head.copy(fileName = "/" + imageUrl)
    val expectedImage     = TestData.bjorn.copy(id = Some(id), images = Seq(expectedImageFile))

    when(imageRepository.withId(id)).thenReturn(Some(expectedImage))
    readService.getDomainImageMetaFromUrl(s"/image-api/raw/id/$id") should be(Success(expectedImage))

    when(imageRepository.getImageFromFilePath(imageUrl)).thenReturn(Some(expectedImage))
    readService.getDomainImageMetaFromUrl(s"/image-api/raw/$imageUrl") should be(Success(expectedImage))

    readService.getDomainImageMetaFromUrl("/image-api/raw/id/apekatt") should be(
      Failure(InvalidUrlException("Could not extract id from id url."))
    )
    readService.getDomainImageMetaFromUrl("/apepe/pawpda/pleps.jpg") should be(
      Failure(InvalidUrlException("Could not extract id or path from url."))
    )
  }

  test("That GET /<id> returns body with original copyright if agreement doesnt exist") {
    implicit val formats: Formats = DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
    val testUrl                   = s"${props.Domain}/image-api/v2/images/1"
    val testRawUrl                = s"${props.Domain}/image-api/raw/Elg.jpg"
    val dateString                = TestData.updated().asString
    val expectedBody =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"$dateString","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testRawUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"CC-BY-NC-SA-4.0","description":"Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International","url":"https://creativecommons.org/licenses/by-nc-sa/4.0/"}, "agreementId":1, "origin":"http://www.scanpix.no","creators":[{"type":"Fotograf","name":"Test Testesen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[{"type":"Leverandør","name":"Leverans Leveransensen"}],"processed":false},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""

    val parsed: JValue = JsonParser.parse(expectedBody)
    val expectedObject: ImageMetaInformationV2 = parsed.extract[api.ImageMetaInformationV2]
    val agreementElg = new ImageMetaInformation(
      id = Some(1),
      titles = List(domain.ImageTitle("Elg i busk", "nb")),
      alttexts = List(domain.ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 1,
          fileName = "Elg.jpg",
          size = 2865539,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          imageMetaId = 1
        )
      ),
      copyright = Copyright(
        TestData.ByNcSa,
        Some("http://www.scanpix.no"),
        List(common.Author("Fotograf", "Test Testesen")),
        List(common.Author("Redaksjonelt", "Kåre Knegg")),
        List(common.Author("Leverandør", "Leverans Leveransensen")),
        None,
        None,
        false
      ),
      tags = List(common.Tag(List("rovdyr", "elg"), "nb")),
      captions = List(domain.ImageCaption("Elg i busk", "nb")),
      updatedBy = "ndla124",
      updated = TestData.updated(),
      created = TestData.updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    when(imageRepository.withId(1)).thenReturn(Some(agreementElg))
    readService.withId(1, None, None) should be(Success(Some(expectedObject)))
  }

  test("That path to raw conversion works with non-ascii characters in paths") {
    reset(imageRepository)
    val id            = 1234L
    val imageUrl      = "Jordbær.jpg"
    val encodedPath   = "Jordb%C3%A6r.jpg"
    val expectedFile  = TestData.bjorn.images.head.copy(fileName = imageUrl)
    val expectedImage = TestData.bjorn.copy(id = Some(id), images = Seq(expectedFile))

    doReturn(Some(expectedImage), Some(expectedImage))
      .when(imageRepository)
      .getImageFromFilePath(eqTo(encodedPath))(any[DBSession])
    readService.getDomainImageMetaFromUrl(s"/image-api/raw/$imageUrl") should be(Success(expectedImage))

    verify(imageRepository, times(1)).getImageFromFilePath(encodedPath)
    verify(imageRepository, times(0)).getImageFromFilePath(imageUrl)
  }

}
