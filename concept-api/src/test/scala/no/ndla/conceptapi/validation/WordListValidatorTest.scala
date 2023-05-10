/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.validation

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}
import no.ndla.conceptapi.model.domain.{ConceptType, WordExample, WordList, WordType}

class WordListValidatorTest extends UnitSuite with TestEnvironment {
  test("that WordListValidator fails if ConceptType is concept and wordList is defined") {
    val wordExamples = WordExample(example = "hei hei", language = "nb")
    val wordList     = WordList(wordType = WordType.NOUN, originalLanguage = "nb", examples = List(List(wordExamples)))
    val validationError =
      WordListValidator.validateWordList(maybeWordList = Some(wordList), conceptType = ConceptType.CONCEPT)

    validationError.get.field should be("conceptType")
    validationError.get.message should be(
      s"conceptType needs to be of type ${ConceptType.WORDCLASS} when wordList is defined"
    )
  }
  test("that WordListValidator fails if ConceptType is wordclass and wordList is None") {
    val validationError =
      WordListValidator.validateWordList(maybeWordList = None, conceptType = ConceptType.WORDCLASS)

    validationError.get.field should be("wordList")
    validationError.get.message should be(
      s"wordList field must be defined when conceptType is of type ${ConceptType.WORDCLASS}"
    )
  }

  test("that WordListValidator gives no errors when ConceptType is concept and wordList is not defined") {
    val validationError =
      WordListValidator.validateWordList(maybeWordList = None, conceptType = ConceptType.CONCEPT)
    validationError should be(None)
  }

  test("that WordListValidator gives no errors when ConceptType is wordclass and wordList is defined") {
    val wordExamples = WordExample(example = "hei hei", language = "nb")
    val wordList     = WordList(wordType = WordType.NOUN, originalLanguage = "nb", examples = List(List(wordExamples)))
    val validationError =
      WordListValidator.validateWordList(maybeWordList = Some(wordList), conceptType = ConceptType.WORDCLASS)
    validationError should be(None)
  }
}
