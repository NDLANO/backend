package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.learningpathapi.db.util.{LpDocumentRow, StepDocumentRow}

class V48__AddContributorsToLearningStepTest extends UnitSuite with TestEnvironment {

  test("that license is moved to copyright with empty contributors") {
    val migration   = new V48__AddContributorsToLearningStep
    val oldDocument =
      """
        |{
        |  "license": "CC BY-NC-SA 4.0",
        |  "title": "Test Step",
        |  "description": "This is a test step."
        |}
        |""".stripMargin
    val oldDocumentNoLicense =
      """
        |{
        |  "title": "Test Step",
        |  "description": "This is a test step."
        |}
        |""".stripMargin

    val expectedDocument =
      """
        |{
        |  "copyright": { "license": "CC BY-NC-SA 4.0", "contributors": [] },
        |  "title": "Test Step",
        |  "description": "This is a test step."
        |}
        |""".stripMargin

    val noLicenseJson     = CirceUtil.unsafeParse(oldDocumentNoLicense)
    val expectedConverted = CirceUtil.unsafeParse(expectedDocument)

    val lpDoc                      = LpDocumentRow(1, oldDocument)
    val stepDoc1                   = StepDocumentRow(1, oldDocument)
    val stepDoc2                   = StepDocumentRow(2, oldDocumentNoLicense)
    val (resultLpDoc, resultSteps) = migration.convertPathAndSteps(lpDoc, List(stepDoc1, stepDoc2))

    resultLpDoc should be(lpDoc)
    val result1Json = CirceUtil.unsafeParse(resultSteps.head.learningStepDocument)
    val result2Json = CirceUtil.unsafeParse(resultSteps(1).learningStepDocument)

    result1Json should be(expectedConverted)
    result2Json should be(noLicenseJson)
  }
}
