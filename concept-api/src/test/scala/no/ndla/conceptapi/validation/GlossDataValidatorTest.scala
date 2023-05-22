/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.validation

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}
import no.ndla.conceptapi.model.domain.{ConceptType, GlossExample, GlossData, GlossType}

class GlossDataValidatorTest extends UnitSuite with TestEnvironment {
  test("that GlossDataValidator fails if ConceptType is concept and glossData is defined") {
    val glossExample = GlossExample(example = "hei hei", language = "nb")
    val glossData = GlossData(
      glossType = GlossType.NOUN,
      originalLanguage = "nb",
      examples = List(List(glossExample)),
      alternatives = Map("zh" -> "a", "pinyin" -> "b")
    )
    val validationError =
      GlossDataValidator.validateGlossData(maybeGlossData = Some(glossData), conceptType = ConceptType.CONCEPT)

    validationError.get.field should be("conceptType")
    validationError.get.message should be(
      s"conceptType needs to be of type ${ConceptType.GLOSS} when glossData is defined"
    )
  }
  test("that GlossDataValidator fails if ConceptType is gloss and glossData is None") {
    val validationError =
      GlossDataValidator.validateGlossData(maybeGlossData = None, conceptType = ConceptType.GLOSS)

    validationError.get.field should be("glossData")
    validationError.get.message should be(
      s"glossData field must be defined when conceptType is of type ${ConceptType.GLOSS}"
    )
  }

  test("that GlossDataValidator gives no errors when ConceptType is concept and glossData is not defined") {
    val validationError =
      GlossDataValidator.validateGlossData(maybeGlossData = None, conceptType = ConceptType.CONCEPT)
    validationError should be(None)
  }

  test("that GlossDataValidator gives no errors when ConceptType is gloss and glossData is defined") {
    val glossExample = GlossExample(example = "hei hei", language = "nb")
    val glossData = GlossData(
      glossType = GlossType.NOUN,
      originalLanguage = "nb",
      examples = List(List(glossExample)),
      alternatives = Map("zh" -> "a", "pinyin" -> "b")
    )
    val validationError =
      GlossDataValidator.validateGlossData(maybeGlossData = Some(glossData), conceptType = ConceptType.GLOSS)
    validationError should be(None)
  }
}
