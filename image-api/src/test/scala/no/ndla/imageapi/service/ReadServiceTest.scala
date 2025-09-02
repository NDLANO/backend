/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain as common
import no.ndla.common.model.domain.ContributorType
import no.ndla.imageapi.model.api.ImageMetaInformationV2DTO
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.model.{InvalidUrlException, api, domain}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doReturn, reset, times, verify, when}
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override implicit lazy val readService: ReadService           = new ReadService
  override implicit lazy val converterService: ConverterService = new ConverterService

  test("That path to id conversion works as expected for id paths") {
    val id                = 1234L
    val imageUrl          = "apekatt.jpg"
    val expectedImageFile = TestData.bjorn.images.get.head.copy(fileName = "/" + imageUrl)
    val expectedImage     = TestData.bjorn.copy(id = Some(id), images = Some(Seq(expectedImageFile)))

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
    val testUrl      = s"${props.Domain}/image-api/v2/images/1"
    val testRawUrl   = s"${props.Domain}/image-api/raw/Elg.jpg"
    val dateString   = TestData.updated().asString
    val expectedBody =
      s"""{
         |  "id":"1",
         |  "metaUrl":"$testUrl",
         |  "title":{"title":"Elg i busk","language":"nb"},
         |  "created":"$dateString",
         |  "createdBy":"ndla124",
         |  "modelRelease":"yes",
         |  "alttext":{"alttext":"Elg i busk","language":"nb"},
         |  "imageUrl":"$testRawUrl",
         |  "size":2865539,
         |  "contentType":"image/jpeg",
         |  "copyright":{
         |    "license":{
         |      "license":"CC-BY-NC-SA-4.0",
         |      "description":"Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International",
         |      "url":"https://creativecommons.org/licenses/by-nc-sa/4.0/"
         |    },
         |    "origin":"http://www.scanpix.no",
         |    "creators":[{"type":"photographer","name":"Test Testesen"}],
         |    "processors":[{"type":"editorial","name":"Kåre Knegg"}],
         |    "rightsholders":[{"type":"supplier","name":"Leverans Leveransensen"}],
         |    "processed":false
         |  },
         |  "tags":{"tags":["rovdyr","elg"],"language":"nb"},
         |  "caption":{"caption":"Elg i busk","language":"nb"},
         |  "supportedLanguages":["nb"]
         |}""".stripMargin

    val expectedObject: ImageMetaInformationV2DTO = CirceUtil.unsafeParseAs[api.ImageMetaInformationV2DTO](expectedBody)
    val imageElg                                  = new ImageMetaInformation(
      id = Some(1),
      titles = List(domain.ImageTitle("Elg i busk", "nb")),
      alttexts = List(domain.ImageAltText("Elg i busk", "nb")),
      images = Some(
        Seq(
          new ImageFileData(
            id = 1,
            fileName = "Elg.jpg",
            size = 2865539,
            contentType = "image/jpeg",
            dimensions = None,
            language = "nb",
            imageMetaId = 1
          )
        )
      ),
      copyright = Copyright(
        TestData.ByNcSa,
        Some("http://www.scanpix.no"),
        List(common.Author(ContributorType.Photographer, "Test Testesen")),
        List(common.Author(ContributorType.Editorial, "Kåre Knegg")),
        List(common.Author(ContributorType.Supplier, "Leverans Leveransensen")),
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

    when(imageRepository.withId(1)).thenReturn(Some(imageElg))
    readService.withId(1, None, None) should be(Success(Some(expectedObject)))
  }

  test("That path to raw conversion works with non-ascii characters in paths") {
    reset(imageRepository)
    val id            = 1234L
    val imageUrl      = "Jordbær.jpg"
    val encodedPath   = "Jordb%C3%A6r.jpg"
    val expectedFile  = TestData.bjorn.images.get.head.copy(fileName = imageUrl)
    val expectedImage = TestData.bjorn.copy(id = Some(id), images = Some(Seq(expectedFile)))

    doReturn(Some(expectedImage), Some(expectedImage))
      .when(imageRepository)
      .getImageFromFilePath(eqTo(encodedPath))(using any[DBSession])
    readService.getDomainImageMetaFromUrl(s"/image-api/raw/$imageUrl") should be(Success(expectedImage))

    verify(imageRepository, times(1)).getImageFromFilePath(encodedPath)
    verify(imageRepository, times(0)).getImageFromFilePath(imageUrl)
  }

  test("That filenames stored with encoded characters works as expected") {
    val imageUrl = "Gr%C3%B8nnsaker%20er%20kilde%20for%20mange%20vitaminer%20(Foto:%20Bj%C3%B8rg%20Aurebekk).jpg"
    val expectedFileName = "Grønnsaker er kilde for mange vitaminer (Foto: Bjørg Aurebekk).jpg"
    when(imageRepository.withId(1)).thenReturn(
      Some(
        TestData.bjorn.copy(images =
          Some(
            Seq(
              TestData.bjorn.images.get.head.copy(fileName = imageUrl)
            )
          )
        )
      )
    )

    readService.getImageFileName(1, Some("nb")) should be(Success(Some(expectedFileName)))

  }

}
