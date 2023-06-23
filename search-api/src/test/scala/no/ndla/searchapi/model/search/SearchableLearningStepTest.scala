/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.model.domain.learningpath.StepType
import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

class SearchableLearningStepTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableLearningStep to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val original1 = SearchableLearningStep(
      stepType = StepType.INTRODUCTION.toString
    )
    val original2 = SearchableLearningStep(
      stepType = StepType.QUIZ.toString
    )
    val original3 = SearchableLearningStep(
      stepType = StepType.TEXT.toString
    )

    val json1         = write(original1)
    val json2         = write(original2)
    val json3         = write(original3)
    val deserialized1 = read[SearchableLearningStep](json1)
    val deserialized2 = read[SearchableLearningStep](json2)
    val deserialized3 = read[SearchableLearningStep](json3)

    deserialized1 should be(original1)
    deserialized2 should be(original2)
    deserialized3 should be(original3)
  }
}
