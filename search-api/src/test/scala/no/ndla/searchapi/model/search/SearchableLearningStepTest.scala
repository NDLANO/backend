/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.StepType
import no.ndla.searchapi.{TestEnvironment, UnitSuite}

class SearchableLearningStepTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableLearningStep to json and deserializing back to object does not change content") {
    val original1 = SearchableLearningStep(
      stepType = StepType.INTRODUCTION.toString
    )
    val original2 = SearchableLearningStep(
      stepType = StepType.QUIZ.toString
    )
    val original3 = SearchableLearningStep(
      stepType = StepType.TEXT.toString
    )

    val json1         = CirceUtil.toJsonString(original1)
    val json2         = CirceUtil.toJsonString(original2)
    val json3         = CirceUtil.toJsonString(original3)
    val deserialized1 = CirceUtil.unsafeParseAs[SearchableLearningStep](json1)
    val deserialized2 = CirceUtil.unsafeParseAs[SearchableLearningStep](json2)
    val deserialized3 = CirceUtil.unsafeParseAs[SearchableLearningStep](json3)

    deserialized1 should be(original1)
    deserialized2 should be(original2)
    deserialized3 should be(original3)
  }
}
