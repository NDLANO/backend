/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain.{Concept, VisualElement}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val service = new ReadService()

  test("Checks that filter by language works as it should") {

    when(publishedConceptRepository.everyTagFromEveryConcept).thenReturn(
      List(
        List(
          domain.ConceptTags(Seq("konge", "bror"), "nb"),
          domain.ConceptTags(Seq("konge", "brur"), "nn"),
          domain.ConceptTags(Seq("king", "bro"), "en"),
          domain.ConceptTags(Seq("zing", "xiongdi"), "zh"),
        ),
        List(
          domain.ConceptTags(Seq("konge", "lol", "meme"), "nb"),
          domain.ConceptTags(Seq("konge", "lel", "meem"), "nn"),
          domain.ConceptTags(Seq("king", "lul", "maymay"), "en"),
          domain.ConceptTags(Seq("zing", "kek", "mimi"), "zh")
        )
      ))

    val result_nb = service.allTagsFromConcepts("nb", fallback = false)
    val result_nn = service.allTagsFromConcepts("nn", fallback = false)
    val result_en = service.allTagsFromConcepts("en", fallback = false)
    val result_zh = service.allTagsFromConcepts("zh", fallback = false)
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
        "<embed data-resource=\"image\" data-resource_id=\"1\" data-alt=\"Alt\" data-size=\"full\" data-align=\"\">",
        "nb"),
      VisualElement("<embed data-resource=\"h5p\" data-path=\"/resource/uuid\" data-title=\"Title\">", "nn")
    )
    when(publishedConceptRepository.withId(anyLong))
      .thenReturn(Some(TestData.sampleConcept.copy(visualElement = visualElements)))
    val concept = service.publishedConceptWithId(id = 1L, language = "nb", fallback = true)
    concept.get.visualElement.get.visualElement should equal(
      "<embed data-resource=\"image\" data-resource_id=\"1\" data-alt=\"Alt\" data-size=\"full\" data-align=\"\" data-url=\"http://api-gateway.ndla-local/image-api/v2/images/1\">")
    val concept2 = service.publishedConceptWithId(id = 1L, language = "nn", fallback = true)
    concept2.get.visualElement.get.visualElement should equal(
      "<embed data-resource=\"h5p\" data-path=\"/resource/uuid\" data-title=\"Title\" data-url=\"https://h5p.ndla.no/resource/uuid\">")

  }
}
