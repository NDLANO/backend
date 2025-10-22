/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import io.circe.parser
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V21__FixContributorTypesTest extends UnitSuite with TestEnvironment {
  test("That copyright has correct contributor types") {
    val migration   = new V21__FixContributorTypes
    val oldDocument = """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "st. olav",
        |        "saint",
        |        "konge"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "titles": [
        |    {
        |      "title": "Olavs død - alterbilde",
        |      "language": "nn"
        |    }
        |  ],
        |  "created": "2019-02-17T15:07:30.000Z",
        |  "updated": "2023-10-31T17:11:47.960Z",
        |  "alttexts": [
        |    {
        |      "alttext": "Hærmenn samles rundt den døde kongen. Illustrasjon.",
        |      "language": "nn"
        |    }
        |  ],
        |  "captions": [
        |    {
        |      "caption": "Olav der Heilige",
        |      "language": "nn"
        |    }
        |  ],
        |  "copyright": {
        |    "origin": "https://commons.wikimedia.org/wiki/File:Olav_der_Heilige06.jpg",
        |    "license": "PD",
        |    "processed": false,
        |    "creators": [
        |      {
        |        "type": "Photographer",
        |        "name": "Fingalo"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": []
        |  },
        |  "createdBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "updatedBy": "YsTwRNHTx5X4hKCrLASNs_Pr",
        |  "editorNotes": [
        |    {
        |      "note": "Added new language 'nn'.",
        |      "timeStamp": "2023-10-31T17:11:47.960Z",
        |      "updatedBy": "YsTwRNHTx5X4hKCrLASNs_Pr"
        |    },
        |    {
        |      "note": "Deleted language 'und'.",
        |      "timeStamp": "2023-10-31T17:12:00.891Z",
        |      "updatedBy": "YsTwRNHTx5X4hKCrLASNs_Pr"
        |    }
        |  ],
        |  "modelReleased": "not-applicable"
        |}
        |""".stripMargin
    val newDocument = """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "st. olav",
        |        "saint",
        |        "konge"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "titles": [
        |    {
        |      "title": "Olavs død - alterbilde",
        |      "language": "nn"
        |    }
        |  ],
        |  "created": "2019-02-17T15:07:30.000Z",
        |  "updated": "2023-10-31T17:11:47.960Z",
        |  "alttexts": [
        |    {
        |      "alttext": "Hærmenn samles rundt den døde kongen. Illustrasjon.",
        |      "language": "nn"
        |    }
        |  ],
        |  "captions": [
        |    {
        |      "caption": "Olav der Heilige",
        |      "language": "nn"
        |    }
        |  ],
        |  "copyright": {
        |    "origin": "https://commons.wikimedia.org/wiki/File:Olav_der_Heilige06.jpg",
        |    "license": "PD",
        |    "processed": false,
        |    "creators": [
        |      {
        |        "type": "photographer",
        |        "name": "Fingalo"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": []
        |  },
        |  "createdBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "updatedBy": "YsTwRNHTx5X4hKCrLASNs_Pr",
        |  "editorNotes": [
        |    {
        |      "note": "Added new language 'nn'.",
        |      "timeStamp": "2023-10-31T17:11:47.960Z",
        |      "updatedBy": "YsTwRNHTx5X4hKCrLASNs_Pr"
        |    },
        |    {
        |      "note": "Deleted language 'und'.",
        |      "timeStamp": "2023-10-31T17:12:00.891Z",
        |      "updatedBy": "YsTwRNHTx5X4hKCrLASNs_Pr"
        |    }
        |  ],
        |  "modelReleased": "not-applicable"
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
