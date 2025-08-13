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

class V43__FixContributorTypesTest extends UnitSuite with TestEnvironment {
  test("That copyright has correct contributor types") {
    val migration   = new V43__FixContributorTypes
    val oldDocument =
      """
        |{
        |  "id": null,
        |  "tags": [
        |    {
        |      "tags": [
        |        "scale",
        |        "geology"
        |      ],
        |      "language": "und"
        |    },
        |    {
        |      "tags": [
        |        "avsetning",
        |        "erosjon",
        |        "geologi"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "scale",
        |        "geology"
        |      ],
        |      "language": "en"
        |    },
        |    {
        |      "tags": [
        |        "geologi"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "owner": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "title": [
        |    {
        |      "title": "Geologiske prosesser",
        |      "language": "nb"
        |    }
        |  ],
        |  "status": "PUBLISHED",
        |  "created": "2020-06-05T07:58:34.000Z",
        |  "message": null,
        |  "duration": 180,
        |  "revision": null,
        |  "copyright": {
        |    "license": "CC-BY-SA-4.0",
        |    "contributors": [
        |      {
        |        "type": "Redaksjonelt",
        |        "name": "Sissel Paaske"
        |      }
        |    ]
        |  },
        |  "isBasedOn": null,
        |  "externalId": null,
        |  "description": [
        |    {
        |      "language": "nb",
        |      "description": "Læringssti om temaet geologiske prosesser."
        |    }
        |  ],
        |  "lastUpdated": "2020-06-05T07:58:34.000Z",
        |  "coverPhotoId": "40683",
        |  "isMyNDLAOwner": false,
        |  "madeAvailable": "2020-06-05T07:58:34.000Z",
        |  "verificationStatus": "CREATED_BY_NDLA"
        |}
        |""".stripMargin
    val newDocument =
      """
        |{
        |  "id": null,
        |  "tags": [
        |    {
        |      "tags": [
        |        "scale",
        |        "geology"
        |      ],
        |      "language": "und"
        |    },
        |    {
        |      "tags": [
        |        "avsetning",
        |        "erosjon",
        |        "geologi"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "scale",
        |        "geology"
        |      ],
        |      "language": "en"
        |    },
        |    {
        |      "tags": [
        |        "geologi"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "owner": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "title": [
        |    {
        |      "title": "Geologiske prosesser",
        |      "language": "nb"
        |    }
        |  ],
        |  "status": "PUBLISHED",
        |  "created": "2020-06-05T07:58:34.000Z",
        |  "message": null,
        |  "duration": 180,
        |  "revision": null,
        |  "copyright": {
        |    "license": "CC-BY-SA-4.0",
        |    "contributors": [
        |      {
        |        "type": "editorial",
        |        "name": "Sissel Paaske"
        |      }
        |    ]
        |  },
        |  "isBasedOn": null,
        |  "externalId": null,
        |  "description": [
        |    {
        |      "language": "nb",
        |      "description": "Læringssti om temaet geologiske prosesser."
        |    }
        |  ],
        |  "lastUpdated": "2020-06-05T07:58:34.000Z",
        |  "coverPhotoId": "40683",
        |  "isMyNDLAOwner": false,
        |  "madeAvailable": "2020-06-05T07:58:34.000Z",
        |  "verificationStatus": "CREATED_BY_NDLA"
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
