/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

import io.circe.parser
import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V28__FixContributorTypesPublishedTest extends UnitSuite with TestEnvironment {
  test("That copyright has correct contributor types") {
    val migration   = new V28__FixContributorTypesPublished
    val oldDocument = """
        |{
        |  "id": 4851,
        |  "tags": [
        |    {
        |      "tags": [
        |        "CNC",
        |        "Teknologi- og industrifag"
        |      ],
        |      "language": "nn"
        |    },
        |    {
        |      "tags": [
        |        "CNC",
        |        "Teknologi- og industrifag"
        |      ],
        |      "language": "nb"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "moturs",
        |      "language": "nn"
        |    },
        |    {
        |      "title": "moturs",
        |      "language": "nb"
        |    }
        |  ],
        |  "status": {
        |    "other": [],
        |    "current": "FOR_APPROVAL"
        |  },
        |  "content": [
        |    {
        |      "content": "Moturs beskriv ei sirkelforma rørsle i motsett retning av urvisaren.\n\nFor programmering i G-kode blir kommando G03 nytta for rørsle i koordinatsystemet, og M03 blir nytta for rotasjonsretninga til spindelen.",
        |      "language": "nn"
        |    },
        |    {
        |      "content": "Moturs beskriver en sirkelformet bevegelse i motsatt retning av urviseren<br/><br/>For programmering i G-kode benyttes kommando G03 for bevegelse i koordinatsystemet, og M03 benyttes for spindelens rotasjonsretning.",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2023-02-27T13:31:42.000Z",
        |  "updated": "2025-02-18T08:21:46.910Z",
        |  "revision": 18,
        |  "copyright": {
        |    "origin": null,
        |    "license": "CC-BY-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "Forfatter",
        |        "name": "Roger Rosmo"
        |      }
        |    ],
        |    "processors": [
        |      {
        |        "type": "korrektur",
        |        "name": "Anne Vagstein"
        |      }
        |    ],
        |    "rightsholders": []
        |  },
        |  "glossData": null,
        |  "updatedBy": [
        |    "-jME11plz_kZ-T8vqKlztgmn",
        |    "TQ4PTLdcsPWRhfNxGVfRAlt9",
        |    "lwkLpeEV_VUmCkly1SJ3WTkg",
        |    "455VO7UP9CLDSUU5TLugvgnc",
        |    "mFfrGSyCOBDWB2FiWlB_fdw7"
        |  ],
        |  "conceptType": "concept",
        |  "editorNotes": [
        |    {
        |      "note": "Updated concept",
        |      "user": "mFfrGSyCOBDWB2FiWlB_fdw7",
        |      "status": {
        |        "other": [],
        |        "current": "FOR_APPROVAL"
        |      },
        |      "timestamp": "2025-02-18T08:21:46.911Z"
        |    }
        |  ],
        |  "responsible": {
        |    "lastUpdated": "2023-06-20T05:55:50.000Z",
        |    "responsibleId": "mFfrGSyCOBDWB2FiWlB_fdw7"
        |  },
        |  "visualElement": []
        |}
        |""".stripMargin
    val newDocument = """
        |{
        |  "id": 4851,
        |  "tags": [
        |    {
        |      "tags": [
        |        "CNC",
        |        "Teknologi- og industrifag"
        |      ],
        |      "language": "nn"
        |    },
        |    {
        |      "tags": [
        |        "CNC",
        |        "Teknologi- og industrifag"
        |      ],
        |      "language": "nb"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "moturs",
        |      "language": "nn"
        |    },
        |    {
        |      "title": "moturs",
        |      "language": "nb"
        |    }
        |  ],
        |  "status": {
        |    "other": [],
        |    "current": "FOR_APPROVAL"
        |  },
        |  "content": [
        |    {
        |      "content": "Moturs beskriv ei sirkelforma rørsle i motsett retning av urvisaren.\n\nFor programmering i G-kode blir kommando G03 nytta for rørsle i koordinatsystemet, og M03 blir nytta for rotasjonsretninga til spindelen.",
        |      "language": "nn"
        |    },
        |    {
        |      "content": "Moturs beskriver en sirkelformet bevegelse i motsatt retning av urviseren<br/><br/>For programmering i G-kode benyttes kommando G03 for bevegelse i koordinatsystemet, og M03 benyttes for spindelens rotasjonsretning.",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2023-02-27T13:31:42.000Z",
        |  "updated": "2025-02-18T08:21:46.910Z",
        |  "revision": 18,
        |  "copyright": {
        |    "origin": null,
        |    "license": "CC-BY-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "writer",
        |        "name": "Roger Rosmo"
        |      }
        |    ],
        |    "processors": [
        |      {
        |        "type": "correction",
        |        "name": "Anne Vagstein"
        |      }
        |    ],
        |    "rightsholders": []
        |  },
        |  "glossData": null,
        |  "updatedBy": [
        |    "-jME11plz_kZ-T8vqKlztgmn",
        |    "TQ4PTLdcsPWRhfNxGVfRAlt9",
        |    "lwkLpeEV_VUmCkly1SJ3WTkg",
        |    "455VO7UP9CLDSUU5TLugvgnc",
        |    "mFfrGSyCOBDWB2FiWlB_fdw7"
        |  ],
        |  "conceptType": "concept",
        |  "editorNotes": [
        |    {
        |      "note": "Updated concept",
        |      "user": "mFfrGSyCOBDWB2FiWlB_fdw7",
        |      "status": {
        |        "other": [],
        |        "current": "FOR_APPROVAL"
        |      },
        |      "timestamp": "2025-02-18T08:21:46.911Z"
        |    }
        |  ],
        |  "responsible": {
        |    "lastUpdated": "2023-06-20T05:55:50.000Z",
        |    "responsibleId": "mFfrGSyCOBDWB2FiWlB_fdw7"
        |  },
        |  "visualElement": []
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
