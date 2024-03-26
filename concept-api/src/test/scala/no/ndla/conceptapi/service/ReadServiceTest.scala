/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain as common
import no.ndla.conceptapi.model.domain.VisualElement
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val service             = new ReadService()
  val userInfo: TokenUser = TokenUser("", Set(CONCEPT_API_WRITE), None)

  test("Checks that filter by language works as it should") {

    when(publishedConceptRepository.everyTagFromEveryConcept).thenReturn(
      List(
        List(
          common.Tag(Seq("konge", "bror"), "nb"),
          common.Tag(Seq("konge", "brur"), "nn"),
          common.Tag(Seq("king", "bro"), "en"),
          common.Tag(Seq("zing", "xiongdi"), "zh")
        ),
        List(
          common.Tag(Seq("konge", "lol", "meme"), "nb"),
          common.Tag(Seq("konge", "lel", "meem"), "nn"),
          common.Tag(Seq("king", "lul", "maymay"), "en"),
          common.Tag(Seq("zing", "kek", "mimi"), "zh")
        )
      )
    )

    val result_nb  = service.allTagsFromConcepts("nb", fallback = false)
    val result_nn  = service.allTagsFromConcepts("nn", fallback = false)
    val result_en  = service.allTagsFromConcepts("en", fallback = false)
    val result_zh  = service.allTagsFromConcepts("zh", fallback = false)
    val result_all = service.allTagsFromConcepts("*", fallback = false)

    result_nb should equal(List("konge", "bror", "lol", "meme"))
    result_nn should equal(List("konge", "brur", "lel", "meem"))
    result_en should equal(List("king", "bro", "lul", "maymay"))
    result_zh should equal(List("zing", "xiongdi", "kek", "mimi"))
    result_all should equal(List("konge", "bror", "lol", "meme"))
  }

  test("that visualElement gets url-property added") {
    val visualElements = Seq(
      VisualElement(
        s"<$EmbedTagName data-resource=\"image\" data-resource_id=\"1\" data-alt=\"Alt\" data-size=\"full\" data-align=\"\" />",
        "nb"
      ),
      VisualElement(s"<$EmbedTagName data-resource=\"h5p\" data-path=\"/resource/uuid\" data-title=\"Title\" />", "nn")
    )
    when(publishedConceptRepository.withId(any))
      .thenReturn(Some(TestData.sampleConcept.copy(visualElement = visualElements)))
    val concept = service.publishedConceptWithId(id = 1L, language = "nb", fallback = true, Some(userInfo))
    concept.get.visualElement.get.visualElement should equal(
      s"<$EmbedTagName data-resource=\"image\" data-resource_id=\"1\" data-alt=\"Alt\" data-size=\"full\" data-align=\"\" data-url=\"http://api-gateway.ndla-local/image-api/v2/images/1\" />"
    )
    val concept2 = service.publishedConceptWithId(id = 1L, language = "nn", fallback = true, Some(userInfo))
    concept2.get.visualElement.get.visualElement should equal(
      s"<$EmbedTagName data-resource=\"h5p\" data-path=\"/resource/uuid\" data-title=\"Title\" data-url=\"https://h5p-test.ndla.no/resource/uuid\" />"
    )

  }
}
