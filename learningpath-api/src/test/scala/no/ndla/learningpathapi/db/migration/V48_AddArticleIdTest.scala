/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.parser
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V48__AddArticleIdTest extends UnitSuite with TestEnvironment {

  test("that article embed urls are converted to articles") {
    val migration   = new V48__AddArticleId
    val oldDocument =
      """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/article-iframe/12345",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/article-iframe/nn/12345",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument =
      """
        |{
        |  "embedUrl": [],
        |  "article": [
        |    {
        |      "id": 12345,
        |      "language": "nb"
        |    },
        |    {
        |      "id": 12345,
        |      "language": "nn"
        |    }
        |  ]
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that node resource urls are converted to articles") {
    val migration   = new V48__AddArticleId
    val oldDocument =
      """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/article-iframe/nb/urn:resource:1:173816/17643",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/article-iframe/nn/urn:resource:1:173816/17643",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument =
      """
        |{
        |  "embedUrl": [],
        |  "article": [
        |    {
        |      "id": 17643,
        |      "language": "nb"
        |    },
        |    {
        |      "id": 17643,
        |      "language": "nn"
        |    }
        |  ]
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that non-article-iframe URLs are left alone") {
    val migration   = new V48__AddArticleId
    val oldDocument =
      """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/nn/article/3997",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/article-iframe/nb/urn:resource:1:173816/17643",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument =
      """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/nn/article/3997",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ],
        |  "article": 17643, 
        |}
        |""".stripMargin

    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
