/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V62__ConvertLearningStepTypeTest extends UnitSuite with TestEnvironment {
  val migration = new V62__ConvertLearningStepType
  test("that article steps removes remaining embedUrls") {
    val document = """
        |{
        | "type": "TEXT",
        | "embedUrl": [
        |   {
        |     "url": "/article-iframe/12345",
        |     "language": "nb",
        |     "embedType": "iframe"
        |   }
        |  ],
        |  "articleId": 1
        |}
        |""".stripMargin

    val expectedDocument = """
        |{
        |   "type": "ARTICLE",
        |   "embedUrl": [],
        |   "articleId": 1
        |}
        |""".stripMargin

    val result = migration.convertStep(document)

    val resultJson   = CirceUtil.unsafeParse(result)
    val expectedJson = CirceUtil.unsafeParse(expectedDocument)
    resultJson should be(expectedJson)
  }

  test("That steps with empty embedUrls are converted to text steps") {

    val document = """
        |{
        | "type": "INTRODUCTION",
        | "embedUrl": [
        |   {
        |     "url": "",
        |     "language": "nb",
        |     "embedType": "iframe"
        |   }
        |  ],
        |  "articleId": null
        |}
        |""".stripMargin

    val expectedDocument = """
         |{
         |   "type": "TEXT",
         |   "embedUrl": [],
         |   "articleId": null
         |}
         |""".stripMargin

    val result = migration.convertStep(document)

    val resultJson   = CirceUtil.unsafeParse(result)
    val expectedJson = CirceUtil.unsafeParse(expectedDocument)
    resultJson should be(expectedJson)
  }

  test("That steps containing embedUrls are converted to EXTERNAL and stripped of empty embed urls") {

    val document = """
         |{
         | "type": "INTRODUCTION",
         | "embedUrl": [
         |   {
         |     "url": "/article-iframe/12345",
         |     "language": "nb",
         |     "embedType": "iframe"
         |   },
         |   {
         |    "url": "",
         |    "language": "nn",
         |    "embedType": "iframe"
         |   }
         |  ],
         |  "articleId": null
         |}
         |""".stripMargin

    val expectedDocument = """
         |{
         |   "type": "EXTERNAL",
         |   "embedUrl": [
         |    {
         |     "url": "/article-iframe/12345",
         |     "language": "nb",
         |     "embedType": "iframe"
         |    }
         |   ],
         |   "articleId": null
         |}
         |""".stripMargin

    val result = migration.convertStep(document)

    val resultJson   = CirceUtil.unsafeParse(result)
    val expectedJson = CirceUtil.unsafeParse(expectedDocument)
    resultJson should be(expectedJson)

  }

}
