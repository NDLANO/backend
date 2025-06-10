/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import no.ndla.common.model.domain.frontpage.VisualElementType.Image
import no.ndla.common.model.domain.frontpage.{AboutSubject, MetaDescription, MovieThemeName, VisualElement}
import no.ndla.frontpageapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.language.Language
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.Success

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val writeService: WriteService = new WriteService

  test("That language is deleted for subject page") {
    val subjectPage = TestData.domainSubjectPage.copy(
      about =
        TestData.domainSubjectPage.about ++ Seq(AboutSubject("Foo", "Bar", "nn", VisualElement(Image, "123", None))),
      metaDescription = TestData.domainSubjectPage.metaDescription ++ Seq(MetaDescription("Description", "nn"))
    )
    when(subjectPageRepository.withId(any)).thenReturn(Success(Some(subjectPage)))
    when(subjectPageRepository.updateSubjectPage(any)(any)).thenAnswer(i => Success(i.getArgument(0)))

    val result = writeService.deleteSubjectPageLanguage(subjectPage.id.get, "nn")
    result should be(
      Success(
        ConverterService
          .toApiSubjectPage(TestData.domainSubjectPage, Language.NoLanguage, fallback = true)
          .failIfFailure
      )
    )
  }

  test("That deleting last language for subject page throws exception") {
    when(subjectPageRepository.withId(any)).thenReturn(Success(Some(TestData.domainSubjectPage)))
    when(subjectPageRepository.updateSubjectPage(any)(any)).thenAnswer(i => Success(i.getArgument(0)))

    val result = writeService.deleteSubjectPageLanguage(TestData.domainSubjectPage.id.get, "nb")
    result.isFailure should be(true)
  }

  test("That language is deleted for film front page") {
    val filmFrontPage = TestData.domainFilmFrontPage.copy(
      about =
        TestData.domainFilmFrontPage.about ++ Seq(AboutSubject("Foo", "Bar", "nn", VisualElement(Image, "123", None))),
      movieThemes = TestData.domainFilmFrontPage.movieThemes.map(movieTheme =>
        movieTheme.copy(name = movieTheme.name ++ Seq(MovieThemeName("FooBar", "nn")))
      )
    )
    when(filmFrontPageRepository.get(any)).thenReturn(Some(filmFrontPage))
    when(filmFrontPageRepository.update(any)(any)).thenAnswer(i => Success(i.getArgument(0)))

    val result = writeService.deleteFilmFrontPageLanguage("nn")
    result should be(Success(ConverterService.toApiFilmFrontPage(TestData.domainFilmFrontPage, None)))
  }

  test("That deleting last language for film front page throws exception") {
    val filmFrontPage = TestData.domainFilmFrontPage.copy(
      about = TestData.domainFilmFrontPage.about.filter(_.language == "nb"),
      movieThemes = TestData.domainFilmFrontPage.movieThemes.map(movieTheme =>
        movieTheme.copy(name = movieTheme.name.filter(_.language == "nb"))
      )
    )
    when(filmFrontPageRepository.get(any)).thenReturn(Some(filmFrontPage))
    when(filmFrontPageRepository.update(any)(any)).thenAnswer(i => Success(i.getArgument(0)))

    val result = writeService.deleteFilmFrontPageLanguage("nb")
    result.isFailure should be(true)
  }
}
