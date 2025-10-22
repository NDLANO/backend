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

class V55__AddArticleIdTest extends UnitSuite with TestEnvironment {

  val migration = new V55__AddArticleId
  test("that article iframe embed urls are converted to articles") {
    val oldDocument = """
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

    val expectedDocument = """
        |{
        |   "embedUrl": [],
        |   "articleId": 12345
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that articles are recognized even with language prefix") {
    val oldDocument = """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/nb/article/12345",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/nn/article/nn/12345",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument = """
        |{
        |   "embedUrl": [],
        |   "articleId": 12345
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that regular article embed urls are converted to articles") {
    val oldDocument = """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/article/12345",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/article/nn/12345",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument = """
        |{
        |   "embedUrl": [],
        |   "articleId": 12345
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that node resource urls are converted to articles") {
    val oldDocument = """
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

    val expectedDocument = """
        |{
        |   "embedUrl": [],
        |   "articleId": 17643
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that the first article ID is chosen") {
    val oldDocument = """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/article-iframe/123",
        |      "language": "nb",
        |      "embedType": "iframe"
        |    },
        |    {
        |      "url": "/article-iframe/nn/456",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument = """
        |{
        |   "embedUrl": [],
        |   "articleId": 123
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that non-article-iframe URLs are left alone") {
    val oldDocument = """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/nb/node/45437?fag=35",
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

    val expectedDocument = """
        |{
        |  "embedUrl": [
        |    {
        |      "url": "/nb/node/45437?fag=35",
        |      "language": "nn",
        |      "embedType": "iframe"
        |    }
        |  ],
        |  "articleId": 17643
        |}
        |""".stripMargin

    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
