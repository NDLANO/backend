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

class V59__CopyContributorsToLearningStepTest extends UnitSuite with TestEnvironment {

  test("that license is moved to copyright with empty contributors") {
    val migration    = new V59__CopyContributorsToLearningStep
    val learningpath = """
        |{
        |  "copyright": { "license": "CC BY-NC-SA 4.0", "contributors": [{"name":"Forfatter","type":"writer"}]},
        |  "title": [{"title":"Test Step", "language":"nb"}],
        |  "description": [{"description":"This is a test step.", "language":"nb"}],
        |  "isMyNDLAOwner": true
        |}
        |""".stripMargin

    val oldStepNoContributors = """
        |{
        |  "title": [{"title":"Test Step", "language":"nb"}],
        |  "description": [{"description":"This is a test step.", "language":"nb"}],
        |  "copyright": { "license": "CC BY-NC-SA 4.0", "contributors": []},
        |  "type": "TEXT",
        |  "embedUrl": [],
        |  "articleId": null
        |}
        |""".stripMargin

    val expectedDocument = """
        |{
        |  "title": [{"title":"Test Step", "language":"nb"}],
        |  "description": [{"description":"This is a test step.", "language":"nb"}],
        |  "copyright": { "license": "CC BY-NC-SA 4.0", "contributors": [{"name":"Forfatter","type":"writer"}]},
        |  "type": "TEXT",
        |  "embedUrl": [],
        |  "articleId": null
        |}
        |""".stripMargin

    val expectedConverted = CirceUtil.unsafeParse(expectedDocument)

    val lpDoc                      = LpDocumentRow(1, learningpath)
    val stepDoc1                   = StepDocumentRow(1, oldStepNoContributors)
    val (resultLpDoc, resultSteps) = migration.convertPathAndSteps(lpDoc, List(stepDoc1))

    resultLpDoc should be(lpDoc)
    val result1Json = CirceUtil.unsafeParse(resultSteps.head.learningStepDocument)

    result1Json should be(expectedConverted)
  }
}
