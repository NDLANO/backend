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

class V44__AddTargetAndRelToAnchorsTest extends UnitSuite with TestEnvironment {

  test("that urls are converted to anchors with target and rel") {
    val migration   = new V44__AddTargetAndRelToAnchors
    val oldDocument =
      """
        |{
        |  "description": [
        |    {
        |      "description": "Læringssti om <a href=\"https://example.com\">temaet</a> geologiske prosesser.",
        |      "language": "nb"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expectedDocument =
      """
        |{
        |  "description": [
        |    {
        |      "description": "Læringssti om <a href=\"https://example.com\" target=\"_blank\" rel=\"noopener noreferrer\">temaet</a> geologiske prosesser.",
        |      "language": "nb"
        |    }
        |  ]
        |}
        |""".stripMargin
    val expected = parser.parse(expectedDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("that no html descriptions are left alone") {
    val migration   = new V44__AddTargetAndRelToAnchors
    val oldDocument =
      """
        |{
        |  "description": [
        |    {
        |      "description": "Læringssti om temaet geologiske prosesser.",
        |      "language": "nb"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expected = parser.parse(oldDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
