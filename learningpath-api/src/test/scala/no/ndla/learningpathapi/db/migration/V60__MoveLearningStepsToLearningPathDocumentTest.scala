/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V60__MoveLearningStepsToLearningPathDocumentTest extends UnitSuite with TestEnvironment {
  test("that learningsteps are embedded in learningpath document with metadata and ordering") {
    val migration = new V60__MoveLearningStepsToLearningPathDocument

    val learningPathDocument = """
        |{
        |  "title": "Test path"
        |}
        |""".stripMargin

    val stepZero = StepDocumentRowWithMeta(
      learningStepId = 1,
      learningPathId = 10,
      revision = 1,
      externalId = Some("ext-1"),
      learningStepDocument = """
          |{
          |  "seqNo": 0,
          |  "title": "Step 0"
          |}
          |""".stripMargin,
    )

    val stepTwo = StepDocumentRowWithMeta(
      learningStepId = 2,
      learningPathId = 10,
      revision = 3,
      externalId = None,
      learningStepDocument = """
          |{
          |  "seqNo": 2,
          |  "title": "Step 2"
          |}
          |""".stripMargin,
    )

    val updatedDocument = migration.mergeLearningSteps(learningPathDocument, List(stepTwo, stepZero))

    val expectedDocument = """
        |{
        |  "title": "Test path",
        |  "learningsteps": [
        |    {
        |      "seqNo": 0,
        |      "title": "Step 0",
        |      "id": 1,
        |      "revision": 1,
        |      "learningPathId": 10,
        |      "externalId": "ext-1"
        |    },
        |    {
        |      "seqNo": 2,
        |      "title": "Step 2",
        |      "id": 2,
        |      "revision": 3,
        |      "learningPathId": 10
        |    }
        |  ]
        |}
        |""".stripMargin

    val resultJson   = CirceUtil.unsafeParse(updatedDocument)
    val expectedJson = CirceUtil.unsafeParse(expectedDocument)

    resultJson should be(expectedJson)
  }
}
