package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.db.util.{LpDocumentRow, StepDocumentRow}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V49__FixLanguageMyNDLALearningPathTest extends UnitSuite, TestEnvironment {
  test("that all other languages than nb are removed if learning path is from My NDLA") {
    val migration       = new V49__FixLanguageMyNDLALearningPath
    val oldPathDocument = """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "kul",
        |        "fett"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "kul",
        |        "fett"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "Potetmos",
        |      "language": "nb"
        |    },
        |    {
        |      "title": "Potetmos",
        |      "language": "nn"
        |    }
        |  ],
        |  "description": [
        |    {
        |      "language": "nb",
        |      "description": "Hvordan lage den perfekte potetmos."
        |    },
        |    {
        |      "language": "nn",
        |      "description": "Hvordan lage den perfekte potetmos."
        |    }
        |  ],
        |  "isMyNDLAOwner": true
        |}
        |""".stripMargin
    val oldStepDocument =
      """
        |{
        |  "title": [
        |    {
        |      "title": "Utforske",
        |      "language": "nn"
        |    },
        |    {
        |      "title": "Utforske",
        |      "language": "nb"
        |    }
        |  ],
        |  "embedUrl": [
        |    {
        |      "url": "/article-iframe/nn/article/1",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/article-iframe/nb/article/1",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    }
        |  ],
        |  "description": [
        |    {
        |      "language": "nn",
        |      "description": "Utforsk potetmosens verden."
        |    },
        |    {
        |      "language": "nb",
        |      "description": "Utforsk potetmosens verden."
        |    }
        |  ],
        |  "introduction": [
        |    {
        |      "introduction": "Dette er en introduksjon til potetmos.",
        |      "language": "nn"
        |    },
        |    {
        |      "introduction": "Dette er en introduksjon til potetmos.",
        |      "language": "nb"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedPathDocument = """
          |{
          |  "tags": [
          |    {
          |      "tags": [
          |        "kul",
          |        "fett"
          |      ],
          |      "language": "nb"
          |    }
          |  ],
          |  "title": [
          |    {
          |      "title": "Potetmos",
          |      "language": "nb"
          |    }
          |  ],
          |  "description": [
          |    {
          |      "language": "nb",
          |      "description": "Hvordan lage den perfekte potetmos."
          |    }
          |  ],
          |  "isMyNDLAOwner": true
          |}
        """.stripMargin
    val expectedStepDocument = """
          |{
          |  "title": [
          |    {
          |      "title": "Utforske",
          |      "language": "nb"
          |    }
          |  ],
          |  "embedUrl": [
          |    {
          |      "url": "/article-iframe/nb/article/1",
          |      "language": "nb",
          |      "embedType": "iframe"
          |    }
          |  ],
          |  "description": [
          |    {
          |      "language": "nb",
          |      "description": "Utforsk potetmosens verden."
          |    }
          |  ],
          |  "introduction": [
          |    {
          |      "introduction": "Dette er en introduksjon til potetmos.",
          |      "language": "nb"
          |    }
          |  ]
          |}
        """.stripMargin

    val expectedPathJson = CirceUtil.tryParse(expectedPathDocument).get
    val expectedStepJson = CirceUtil.tryParse(expectedStepDocument).get

    val (lpData, List(stepData)) =
      migration.convertPathAndSteps(LpDocumentRow(1, oldPathDocument), List(StepDocumentRow(1, oldStepDocument)))

    val resultPathJson = CirceUtil.tryParse(lpData.learningPathDocument).get
    val resultStepJson = CirceUtil.tryParse(stepData.learningStepDocument).get
    resultPathJson should be(expectedPathJson)
    resultStepJson should be(expectedStepJson)
  }

  test("that learning paths from My NDLA with single language are unmodified") {
    val migration    = new V49__FixLanguageMyNDLALearningPath
    val pathDocument = """
        |{
        |  "title": [
        |    {
        |      "title": "Kriminalitet",
        |      "language": "nn"
        |    }
        |  ],
        |  "isMyNDLAOwner": false
        |}
        |""".stripMargin
    val stepDocument = """
          |{
          |  "title": [
          |    {
          |      "title": "Utforske",
          |      "language": "nn"
          |    }
          |  ]
          |}
        """.stripMargin

    val lpData    = LpDocumentRow(1, pathDocument)
    val stepDatas = List(StepDocumentRow(1, stepDocument))
    migration.convertPathAndSteps(lpData, stepDatas) should be((lpData, stepDatas))
  }

  test("that learning paths not from My NDLA retain languages") {
    val migration    = new V49__FixLanguageMyNDLALearningPath
    val pathDocument = """
        |{
        |  "title": [
        |    {
        |      "title": "Kriminalitet",
        |      "language": "nb"
        |    },
        |    {
        |      "title": "Kriminalitet",
        |      "language": "nn"
        |    }
        |  ],
        |  "isMyNDLAOwner": false
        |}
        |""".stripMargin
    val stepDocument = """
          |{
          |  "title": [
          |    {
          |      "title": "Utforske",
          |      "language": "nb"
          |    },
          |    {
          |      "title": "Utforske",
          |      "language": "nn"
          |    }
          |  ]
          |}
        """.stripMargin

    val lpData    = LpDocumentRow(1, pathDocument)
    val stepDatas = List(StepDocumentRow(1, stepDocument))
    migration.convertPathAndSteps(lpData, stepDatas) should be((lpData, stepDatas))
  }
}
