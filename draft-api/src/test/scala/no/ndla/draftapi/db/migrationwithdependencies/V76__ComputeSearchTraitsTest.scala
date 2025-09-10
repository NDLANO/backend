/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import no.ndla.common.CirceUtil
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.util.TraitUtil

class V76__ComputeSearchTraitsTest extends UnitSuite with TestEnvironment {
  override implicit lazy val traitUtil: TraitUtil = new TraitUtil
  val migration                                   = new V76__ComputeSearchTraits()
  test("that migration does its job") {
    val before = {
      s"""
         |{
         |  "content": [
         |    {
         |      "content": "Skikkelig bra h5p: <$EmbedTagName data-resource=\\"h5p\\" data-path=\\"/resource/id\\"></$EmbedTagName>",
         |      "language": "nb"
         |    },
         |    {
         |      "content": "Fin video <$EmbedTagName data-resource=\\"external\\" data-url=\\"https://youtu.be/id\\"></$EmbedTagName>",
         |      "language": "nn"
         |    }
         |  ]
         |}
         |""".stripMargin
    }

    val expectedStr = {
      s"""
         |{
         |  "content": [
         |    {
         |      "content": "Skikkelig bra h5p: <$EmbedTagName data-resource=\\"h5p\\" data-path=\\"/resource/id\\"></$EmbedTagName>",
         |      "language": "nb"
         |    },
         |    {
         |      "content": "Fin video <$EmbedTagName data-resource=\\"external\\" data-url=\\"https://youtu.be/id\\"></$EmbedTagName>",
         |      "language": "nn"
         |    }
         |  ],
         |  "traits": ["H5P", "VIDEO"]
         |}
         |""".stripMargin
    }

    val resultStr    = migration.convertColumn(before)
    val result       = CirceUtil.unsafeParse(resultStr)
    val expectedJson = CirceUtil.unsafeParse(expectedStr)
    result should be(expectedJson)
  }
}
