/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import io.circe.parser
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V58__FixContributorTypesTest extends UnitSuite with TestEnvironment {
  test("That copyright has correct contributor types") {
    val migration = new V58__FixContributorTypes
    val oldDocument =
      """
        |{
        |  "id": 1,
        |  "tags": [],
        |  "title": [
        |    {
        |      "title": "Title",
        |      "language": "nb"
        |    }
        |  ],
        |  "content": [
        |    {
        |      "content": "<section>Content</section>",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2017-05-29T09:43:41.000Z",
        |  "updated": "2017-07-18T10:21:08.000Z",
        |  "revision": 1,
        |  "copyright": {
        |    "origin": "",
        |    "license": "CC-BY-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "Forfatter",
        |        "name": "Sissel Paaske"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": [
        |      {
        |        "type": "Supplier",
        |        "name": "Cerpus AS"
        |      }
        |    ]
        |  },
        |  "grepCodes": [],
        |  "metaImage": [],
        |  "published": "2017-07-18T10:21:08.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "standard",
        |  "availability": "everyone",
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "Introduction."
        |    }
        |  ],
        |  "revisionDate": "2030-01-01T00:00:00.000Z",
        |  "visualElement": [],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Metabeskrivelse",
        |      "language": "nb"
        |    }
        |  ],
        |  "requiredLibraries": []
        |}
        |""".stripMargin
    val newDocument =
      """
        |{
        |  "id": 1,
        |  "tags": [],
        |  "title": [
        |    {
        |      "title": "Title",
        |      "language": "nb"
        |    }
        |  ],
        |  "content": [
        |    {
        |      "content": "<section>Content</section>",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2017-05-29T09:43:41.000Z",
        |  "updated": "2017-07-18T10:21:08.000Z",
        |  "revision": 1,
        |  "copyright": {
        |    "origin": "",
        |    "license": "CC-BY-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "writer",
        |        "name": "Sissel Paaske"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": [
        |      {
        |        "type": "supplier",
        |        "name": "Cerpus AS"
        |      }
        |    ]
        |  },
        |  "grepCodes": [],
        |  "metaImage": [],
        |  "published": "2017-07-18T10:21:08.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "standard",
        |  "availability": "everyone",
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "Introduction."
        |    }
        |  ],
        |  "revisionDate": "2030-01-01T00:00:00.000Z",
        |  "visualElement": [],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Metabeskrivelse",
        |      "language": "nb"
        |    }
        |  ],
        |  "requiredLibraries": []
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
