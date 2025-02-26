/*
 * Part of NDLA audio-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.db.migration

import io.circe.parser
import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V22__FixContributorTypesTest extends UnitSuite with TestEnvironment {
  test("That copyright has correct contributor types") {
    val migration = new V22__FixContributorTypes
    val oldDocument =
      """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "testtag",
        |        "testtttt"
        |      ],
        |      "language": "nb"
        |    }
        |  ],
        |  "titles": [
        |    {
        |      "title": "test",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2023-10-04T20:30:18.167Z",
        |  "updated": "2023-10-04T20:30:18.167Z",
        |  "audioType": "standard",
        |  "copyright": {
        |    "license": "CC-BY-SA-4.0",
        |    "processed": true,
        |    "creators": [
        |      {
        |        "type": "Komponist",
        |        "name": "Apekatt"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": [
        |      {
        |        "type": "Supplier",
        |        "name": "NRK"
        |      }
        |    ]
        |  },
        |  "filePaths": [
        |    {
        |      "filePath": "T8BBtfD.mp3\"",
        |      "fileSize": 6289221,
        |      "language": "nb",
        |      "mimeType": "audio/mpeg"
        |    }
        |  ],
        |  "updatedBy": "fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK",
        |  "manuscript": [],
        |  "podcastMeta": [],
        |  "supportedLanguages": null
        |}
        |""".stripMargin
    val newDocument =
      """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "testtag",
        |        "testtttt"
        |      ],
        |      "language": "nb"
        |    }
        |  ],
        |  "titles": [
        |    {
        |      "title": "test",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2023-10-04T20:30:18.167Z",
        |  "updated": "2023-10-04T20:30:18.167Z",
        |  "audioType": "standard",
        |  "copyright": {
        |    "license": "CC-BY-SA-4.0",
        |    "processed": true,
        |    "creators": [
        |      {
        |        "type": "composer",
        |        "name": "Apekatt"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": [
        |      {
        |        "type": "supplier",
        |        "name": "NRK"
        |      }
        |    ]
        |  },
        |  "filePaths": [
        |    {
        |      "filePath": "T8BBtfD.mp3\"",
        |      "fileSize": 6289221,
        |      "language": "nb",
        |      "mimeType": "audio/mpeg"
        |    }
        |  ],
        |  "updatedBy": "fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK",
        |  "manuscript": [],
        |  "podcastMeta": [],
        |  "supportedLanguages": null
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
