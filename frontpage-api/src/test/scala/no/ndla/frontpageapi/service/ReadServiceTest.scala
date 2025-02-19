/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import no.ndla.frontpageapi.model.domain
import no.ndla.frontpageapi.model.domain.VisualElementType
import no.ndla.frontpageapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.Success

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService: ReadService = new ReadService

  test("That all subjectpages does not fail on 404 because of language") {
    val norwegianSubjectPage = TestData.domainSubjectPage.copy(
      id = Some(2),
      metaDescription = Seq(
        domain.MetaDescription("hei", "nb")
      ),
      about = Seq(
        domain.AboutSubject("tittel", "besk", "nb", domain.VisualElement(VisualElementType.Image, "", None))
      )
    )
    val englishSubjectPage = TestData.domainSubjectPage.copy(
      id = Some(2),
      metaDescription = Seq(
        domain.MetaDescription("hello", "en")
      ),
      about = Seq(
        domain.AboutSubject("title", "desc", "en", domain.VisualElement(VisualElementType.Image, "1", None))
      )
    )

    when(subjectPageRepository.all(any, any)(any)).thenReturn(Success(List(norwegianSubjectPage, englishSubjectPage)))

    val result = readService.subjectPages(1, 10, "en", fallback = false)
    result.get.map(_.id) should be(List(2))
  }
}
