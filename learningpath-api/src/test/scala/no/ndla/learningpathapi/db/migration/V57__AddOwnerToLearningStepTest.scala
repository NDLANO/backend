/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.db.util.{LpDocumentRow, StepDocumentRow}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V57__AddOwnerToLearningStepTest extends UnitSuite with TestEnvironment {

  test("that learningstep gets dates from learningpath") {
    val migration    = new V57__AddOwnerToLearningStep
    val learningpath = """
        |{
        |  "license": "CC BY-NC-SA 4.0",
        |  "title": "Test path",
        |  "description": "This is a test path.",
        |  "created": "2024-01-01T00:00:00Z",
        |  "owner": "me"
        |}
        |""".stripMargin
    val stepWithoutOwner = """
        |{
        |  "title": "Test Step",
        |  "description": "This is a test step."
        |}
        |""".stripMargin

    val expectedDocument = """
        |{
        |  "title": "Test Step",
        |  "description": "This is a test step.",
        |  "owner": "me"
        |}
        |""".stripMargin

    val expectedConverted = CirceUtil.unsafeParse(expectedDocument)

    val lpDoc                      = LpDocumentRow(1, learningpath)
    val stepDoc1                   = StepDocumentRow(1, stepWithoutOwner)
    val (resultLpDoc, resultSteps) = migration.convertPathAndSteps(lpDoc, List(stepDoc1))

    resultLpDoc should be(lpDoc)
    val result1Json = CirceUtil.unsafeParse(resultSteps.head.learningStepDocument)

    result1Json should be(expectedConverted)
  }
}
