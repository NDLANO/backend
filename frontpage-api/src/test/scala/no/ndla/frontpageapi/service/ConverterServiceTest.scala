/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.service

import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.model.domain
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, NotFoundException}
import no.ndla.frontpageapi.model.domain.{AboutSubject, Errors, MetaDescription, VisualElement, VisualElementType}
import no.ndla.frontpageapi.{FrontpageApiProperties, TestData, TestEnvironment, UnitSuite}

import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  test("toApiSubjectPage should convert visual element id to url") {
    val visualElement = TestData.domainSubjectPage.about.head.visualElement.copy(`type` = VisualElementType.Image)
    val about = TestData.domainSubjectPage.about.map(_.copy(visualElement = visualElement))
    val page = TestData.domainSubjectPage.copy(about = about)

    ConverterService.toApiSubjectPage(page, "nb").get.about.get.visualElement.url should equal(
      s"http://api-gateway.ndla-local/image-api/raw/id/${visualElement.id}")

    val visualElement2 = TestData.domainSubjectPage.about.head.visualElement.copy(`type` = VisualElementType.Brightcove)
    val about2 = TestData.domainSubjectPage.about.map(_.copy(visualElement = visualElement2))
    val page2 = TestData.domainSubjectPage.copy(about = about2)

    val expected =
      s"https://players.brightcove.net/${FrontpageApiProperties.BrightcoveAccountId}/${FrontpageApiProperties.BrightcovePlayer}_default/index.html?videoId=${visualElement2.id}"
    ConverterService.toApiSubjectPage(page2, "nb").get.about.get.visualElement.url should equal(expected)
  }

  test("toDomainSubjectPage should return a failure if visual element type is invalid") {
    val visualElement = TestData.apiNewSubjectPage.about.head.visualElement.copy(`type` = "not an image")
    val about = TestData.apiNewSubjectPage.about.map(_.copy(visualElement = visualElement))
    val page = TestData.apiNewSubjectPage.copy(about = about)

    val Failure(res: Errors.ValidationException) = ConverterService.toDomainSubjectPage(page)
    res.message should equal("'not an image' is an invalid visual element type")
  }

  test("toDomainSubjectPage should return a success if visual element type is valid") {
    val visualElement = TestData.apiNewSubjectPage.about.head.visualElement.copy(`type` = "image")
    val about = TestData.apiNewSubjectPage.about.map(_.copy(visualElement = visualElement))
    val page = TestData.apiNewSubjectPage.copy(about = about)

    ConverterService.toDomainSubjectPage(page).isSuccess should be(true)
  }

  test("toDomainSubjectPage from patch should convert correctly") {
    val updatedSubjectPage = TestData.apiUpdatedSubjectPage
    val toMergeInto = TestData.domainSubjectPage

    ConverterService.toDomainSubjectPage(toMergeInto, updatedSubjectPage) should be(
      Success(TestData.domainUpdatedSubjectPage))
  }

  test("toDomainSubjectPage updates meta description correctly") {
    val updateWith = UpdatedSubjectFrontPageData(
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some(List(NewOrUpdatedMetaDescription("oppdatert meta", "nb"))),
      None,
      None,
      None,
      None,
      None
    )

    ConverterService.toDomainSubjectPage(TestData.domainSubjectPage, updateWith).get.metaDescription should be(
      Seq(MetaDescription("oppdatert meta", "nb")))
  }

  test("toDomainSubjectPage updates aboutSubject correctly") {
    val updateWith = UpdatedSubjectFrontPageData(
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some(
        List(
          NewOrUpdatedAboutSubject("oppdatert tittel",
                                   "oppdatert beskrivelse",
                                   "nb",
                                   NewOrUpdatedVisualElement("image", "1", None)))),
      None,
      None,
      None,
      None,
      None,
      None
    )

    ConverterService.toDomainSubjectPage(TestData.domainSubjectPage, updateWith).get.about should be(
      Seq(
        AboutSubject("oppdatert tittel",
                     "oppdatert beskrivelse",
                     "nb",
                     VisualElement(VisualElementType.Image, "1", None))))
  }

  test("toDomainSubjectPage adds new language correctly") {
    val updateWith = UpdatedSubjectFrontPageData(
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some(
        List(
          NewOrUpdatedAboutSubject("About Social studies",
                                   "This is social studies",
                                   "en",
                                   NewOrUpdatedVisualElement("image", "123", None)))),
      Some(
        List(
          NewOrUpdatedMetaDescription("meta description", "en")
        )),
      None,
      None,
      None,
      None,
      None
    )

    ConverterService.toDomainSubjectPage(TestData.domainSubjectPage, updateWith) should be(
      Success(TestData.domainSubjectPage.copy(
        about = Seq(
          domain.AboutSubject("Om Samfunnsfag",
                              "Dette er samfunnsfag",
                              "nb",
                              domain.VisualElement(VisualElementType.Image, "123", Some("alt text"))),
          domain.AboutSubject("About Social studies",
                              "This is social studies",
                              "en",
                              domain.VisualElement(VisualElementType.Image, "123", None),
          )
        ),
        metaDescription = Seq(domain.MetaDescription("meta", "nb"), domain.MetaDescription("meta description", "en"))
      )))
  }

  test("toApiSubjectPage failure if subject not found in specified language without fallback") {
    ConverterService.toApiSubjectPage(TestData.domainSubjectPage, "hei", fallback = false) should be(
      Failure(
        LanguageNotFoundException(
          s"The subjectpage with id ${TestData.domainSubjectPage.id.get} and language hei was not found",
          TestData.domainSubjectPage.supportedLanguages)
      )
    )
  }

  test("toApiSubjectPage success if subject not found in specified language, but with fallback") {
    ConverterService.toApiSubjectPage(TestData.domainSubjectPage, "hei", fallback = true) should be(
      Success(TestData.apiSubjectPage)
    )
  }

  test("Should get all languages if nothing is specified") {
    val apiFilmFrontPage = ConverterService.toApiFilmFrontPage(TestData.domainFilmFrontPage, None)
    apiFilmFrontPage.about.length should equal(2)
    apiFilmFrontPage.about.map(_.language) should equal(Seq("nb", "en"))
  }

  test("Should get only specified language") {
    val apiFilmFrontPage = ConverterService.toApiFilmFrontPage(TestData.domainFilmFrontPage, Some("nb"))
    apiFilmFrontPage.about.length should equal(1)
    apiFilmFrontPage.about.map(_.language) should equal(Seq("nb"))

  }
}
