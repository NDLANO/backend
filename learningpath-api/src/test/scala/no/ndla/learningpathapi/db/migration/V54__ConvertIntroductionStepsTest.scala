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

class V54__ConvertIntroductionStepsTest extends UnitSuite, TestEnvironment {
  test("That introduction steps are moved to learningpath introduction") {
    val migration       = new V54__ConvertIntroductionSteps
    val oldPathDocument = """
        |{
        |   "introduction": []
        |}
        |""".stripMargin

    val oldStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 0,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nn",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   },
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "introduction": [
          |    {
            |      "introduction": "<section><p>Utforsk potetmosens verden.</p></section>",
          |      "language": "nn"
          |    },
          |    {
          |      "introduction": "<section><p>Utforsk potetmosens verden.</p></section>",
          |      "language": "nb"
          |    }
          |  ]
          |}
        """.stripMargin

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get

    val (lpData, stepData) =
      migration.convertPathAndSteps(LpDocumentRow(1, oldPathDocument), List(StepDocumentRow(1, oldStepDocument)))

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    resultPathJson should be(expectedPathJson)
    stepData should be(List.empty)
  }
  test("That deleted steps are ignored") {

    val migration       = new V54__ConvertIntroductionSteps
    val oldPathDocument = """
        |{
        |   "introduction": []
        |}
        |""".stripMargin

    val deletedStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": "0",
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Jeg er først men jeg er slettet</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val oldStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 1,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nn",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   },
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "introduction": [
          |    {
          |      "introduction": "<section><p>Utforsk potetmosens verden.</p></section>",
          |      "language": "nn"
          |    },
          |    {
          |      "introduction": "<section><p>Utforsk potetmosens verden.</p></section>",
          |      "language": "nb"
          |    }
          |  ]
          |}
        """.stripMargin

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get

    val (lpData, stepData) = migration.convertPathAndSteps(
      LpDocumentRow(1, oldPathDocument),
      List(StepDocumentRow(1, deletedStepDocument), StepDocumentRow(2, oldStepDocument)),
    )

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    resultPathJson should be(expectedPathJson)
    stepData.size should be(1)
    stepData.head should be(StepDocumentRow(1, deletedStepDocument))
  }
  test("That steps with embed urls are ignored") {
    val migration       = new V54__ConvertIntroductionSteps
    val oldPathDocument = """
        |{
        |   "introduction": []
        |}
        |""".stripMargin

    val oldStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 0,
        | "status": "ACTIVE",
        | "embedUrl": [
        |   {
        |     "url": "https://nrk.no",
        |     "language": "nb",
        |     "embedType": "external"
        |   }
        ],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nn",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   },
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "introduction": []
          |}
        """.stripMargin

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get

    val (lpData, stepData) =
      migration.convertPathAndSteps(LpDocumentRow(1, oldPathDocument), List(StepDocumentRow(1, oldStepDocument)))

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    resultPathJson should be(expectedPathJson)
    stepData.size should be(1)
    stepData.head should be(StepDocumentRow(1, oldStepDocument))
  }
  test("That introduction steps that are not the first step are ignored") {
    val migration = new V54__ConvertIntroductionSteps

    val oldPathDocument = """
        |{
        |   "introduction": []
        |}
        |""".stripMargin

    val textDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 0,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nn",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   },
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val introDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 1,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nn",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   },
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "introduction": []
          |}
        """.stripMargin

    val (lpData, stepData) = migration.convertPathAndSteps(
      LpDocumentRow(1, oldPathDocument),
      List(StepDocumentRow(1, textDocument), StepDocumentRow(2, introDocument)),
    )

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    resultPathJson should be(expectedPathJson)
    stepData should be(List(StepDocumentRow(1, textDocument), StepDocumentRow(2, introDocument)))
  }
  test("That steps with introduction and description are ignored") {
    val migration       = new V54__ConvertIntroductionSteps
    val oldPathDocument = """
        |{
        |   "introduction": []
        |}
        |""".stripMargin

    val oldStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 0,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [
        |   {
        |     "introduction": "Hallo",
        |     "language": "nb"
        |   }
        ],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "introduction": []
          |}
        """.stripMargin

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get

    val (lpData, stepData) =
      migration.convertPathAndSteps(LpDocumentRow(1, oldPathDocument), List(StepDocumentRow(1, oldStepDocument)))

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    resultPathJson should be(expectedPathJson)
    stepData.size should be(1)
    stepData.head should be(StepDocumentRow(1, oldStepDocument))
  }
  test("That other seqNo are updated when a step is deleted") {
    val migration = new V54__ConvertIntroductionSteps

    val oldPathDocument = """
        |{
        |   "introduction": []
        |}
        |""".stripMargin

    val firstStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 0,
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val secondStepDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 1,
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk verdenens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val thirdStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 2,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk den aktive verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val fourthStepDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 3,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val fifthStepDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 4,
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk den inaktive verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val firstExpectedStepDocument = """
        |{
        | "type": "INTRODUCTION",
        | "seqNo": 0,
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk potetmosens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val secondExpectedStepDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 1,
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk verdenens verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val thirdExpectedStepDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 2,
        | "status": "ACTIVE",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val fourthExpectedStepDocument = """
        |{
        | "type": "TEXT",
        | "seqNo": 3,
        | "status": "DELETED",
        | "embedUrl": [],
        | "introduction": [],
        | "description": [
        |   {
        |     "language": "nb",
        |     "description": "<p>Utforsk den inaktive verden.</p>"
        |   }
        | ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "introduction": [
          |   {
          |     "introduction": "<section><p>Utforsk den aktive verden.</p></section>",
          |     "language": "nb"
          |   }
          | ]
          |}
        """.stripMargin

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get

    val (lpData, stepData) = migration.convertPathAndSteps(
      LpDocumentRow(1, oldPathDocument),
      List(
        StepDocumentRow(1, firstStepDocument),
        StepDocumentRow(2, secondStepDocument),
        StepDocumentRow(3, thirdStepDocument),
        StepDocumentRow(4, fourthStepDocument),
        StepDocumentRow(5, fifthStepDocument),
      ),
    )

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    resultPathJson should be(expectedPathJson)
    stepData.size should be(4)
    CirceUtil.tryParse(stepData(0).learningStepDocument).get should be(
      CirceUtil.tryParse(firstExpectedStepDocument).get
    )
    CirceUtil.tryParse(stepData(1).learningStepDocument).get should be(
      CirceUtil.tryParse(secondExpectedStepDocument).get
    )
    CirceUtil.tryParse(stepData(2).learningStepDocument).get should be(
      CirceUtil.tryParse(thirdExpectedStepDocument).get
    )
    CirceUtil.tryParse(stepData(3).learningStepDocument).get should be(
      CirceUtil.tryParse(fourthExpectedStepDocument).get
    )
  }
}
