/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migrationwithdependencies

import io.circe.syntax.EncoderOps
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.{Tag, Title}
import no.ndla.common.model.domain.concept.{Concept, ConceptContent, VisualElement}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}

class V23__SubjectNameAsTagsTest extends UnitSuite with TestEnvironment {
  val fakeSubjects: List[TaxonomySubject] = List(
    TaxonomySubject(
      "urn:subject:1",
      "Naturfag",
      List(
        TaxonomyTranslation("Naturfag", "nb"),
        TaxonomyTranslation("Naturfagi", "nn"),
        TaxonomyTranslation("Science", "en"),
        TaxonomyTranslation("科学", "zh"),
        TaxonomyTranslation("Luonddufágga", "sma")
      )
    ),
    TaxonomySubject(
      "urn:subject:2",
      "Matte",
      List(
        TaxonomyTranslation("Matematik", "nb"),
        TaxonomyTranslation("Matematiki", "nn"),
        TaxonomyTranslation("Math", "en")
      )
    )
  )

  val migration = new V23__SubjectNameAsTags(props, prefetchedSubjects = Some(fakeSubjects))

  test("That we can get languages from a concept json string") {
    val concept = TestData.domainConcept.copy(
      title = List(Title("Tittel", "nb")),
      content = List(ConceptContent("Innhold", "sma")),
      tags = List(Tag(List("tag1", "tag2", "tag3"), "nn")),
      visualElement = List(VisualElement("zzz", "en"))
    )
    val languages = migration.getLanguages(concept.asJson)
    languages should be(List("nb", "sma", "nn", "en"))
  }

  test("That adding tags works as expected") {
    val concept = TestData.domainConcept.copy(
      title = List(Title("Tittel", "nb")),
      content = List(ConceptContent("Innhold", "sma")),
      tags = List(Tag(List("nb"), "nb"), Tag(List("nn"), "nn"), Tag(List("en"), "en"), Tag(List("zh"), "zh")),
      visualElement = List(VisualElement("zzz", "en"))
    )

    val result = migration.convertColumn(concept.asJson.noSpaces)
    CirceUtil.unsafeParseAs[Concept](result).tags.sortBy(_.language) should be(
      List(
        Tag(List("nb"), "nb"),
        Tag(List("nn"), "nn"),
        Tag(List("en"), "en"),
        Tag(List("zh"), "zh")
      ).sortBy(_.language)
    )

  }
}
