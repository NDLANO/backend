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

class V31__FixImageVisualElementSizeTest extends UnitSuite with TestEnvironment {
  test("That size 'fullbredde' is changed to 'full' in visualElement") {
    val migration   = new V31__FixImageVisualElementSize
    val oldDocument = """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "Brønn",
        |        "Brønnfag"
        |      ],
        |      "language": "nn"
        |    },
        |    {
        |      "tags": [
        |        "Brønn",
        |        "Brønnfag"
        |      ],
        |      "language": "nb"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "brønnhovudtrykk",
        |      "language": "nn"
        |    },
        |    {
        |      "title": "brønnhodetrykk",
        |      "language": "nb"
        |    }
        |  ],
        |  "status": {
        |    "other": [],
        |    "current": "PUBLISHED"
        |  },
        |  "content": [
        |    {
        |      "content": "Brønnhovudtrykk blir lese av på toppen av brønnen i overgangen frå brønnen til ventiltreet. Det er vanleg å bruke nemninga innestengt trykk i ein stengd brønn og strøymingstrykk i ein produserande brønn.\r\n\nBrønnhovudtrykket blir påverka av hydrostatisk trykk frå reservoaret til brønnhovudet, og strøymingstrykk gjennom produksjonsrøyret i brønnen og strupeventilen på overflata. Ein stengd gassbrønn kan ha eit brønnhovudtrykk som er veldig nært reservoartrykket, men ein stengd oljebrønn vil ha betydeleg lågare brønnhovudtrykk på grunn av den hydrostatiske trykkforskjellen frå reservoaret til toppen av brønnen.  \r\n\nPå engelsk heiter brønnhovudtrykk «wellhead pressure», og innestengt trykk heiter «shut in wellhead pressure». \r",
        |      "language": "nn"
        |    },
        |    {
        |      "content": "Brønnhodetrykk leses av på toppen av brønnen i overgangen fra brønnen til ventiltreet. Det er vanlig å bruke betegnelsene innestengt trykk i en stengt brønn og strømningstrykk i en produserende brønn.\r\n\nBrønnhodetrykket påvirkes av hydrostatisk trykk fra reservoaret til brønnhodet, og strømningstrykk gjennom produksjonsrøret i brønnen og strupeventilen på overflaten. En stengt gassbrønn kan ha et brønnhodetrykk som er veldig nært reservoartrykket, men en stengt oljebrønn vil ha betydelig lavere brønnhodetrykk på grunn av den hydrostatiske trykkforskjellen fra reservoaret til brønnens topp.  \r\n\nPå engelsk omtales brønnhodetrykk som «wellhead pressure», og innestengt trykk omtales som «shut in wellhead pressure». \r",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2018-07-02T10:59:12Z",
        |  "updated": "2021-06-28T06:29:22Z",
        |  "copyright": {
        |    "license": "CC-BY-SA-4.0",
        |    "creators": [
        |      {
        |        "name": "Sissel Paaske",
        |        "type": "writer"
        |      }
        |    ],
        |    "processed": false,
        |    "processors": [
        |      {
        |        "name": "Totaltekst",
        |        "type": "correction"
        |      }
        |    ],
        |    "rightsholders": []
        |  },
        |  "updatedBy": [
        |    "sPHJn0BEtfxw2d2DUpIuS3iY",
        |    "KBAJskRqPXZUv9LFjAbz8btB",
        |    "fFJdhmAbXvoHcph2vw2HfD_1",
        |    "4iOxWUcXV8La8_gC32Up9H0J",
        |    "yUT8Jlal2wT6AbLc0btQ1K5n"
        |  ],
        |  "conceptType": "concept",
        |  "editorNotes": [],
        |  "visualElement": [
        |    {
        |      "language": "nn",
        |      "visualElement": "<ndlaembed data-size=\"fullbredde\" data-align=\"\" data-caption=\"Brønnhodet til brønn A-20 på Sleipner plattformen.\" data-alt=\"Brønnhodet til brønn A-20 på Sleipner plattformen. Foto.\" data-resource_id=\"54741\" data-resource=\"image\"></ndlaembed>"
        |    },
        |    {
        |      "language": "nb",
        |      "visualElement": "<ndlaembed data-size=\"fullbredde\" data-align=\"\" data-caption=\"Brønnhodet til brønn A-20 på Sleipner plattformen.\" data-alt=\"Brønnhodet til brønn A-20 på Sleipner plattformen. Foto.\" data-resource_id=\"54741\" data-resource=\"image\"></ndlaembed>"
        |    }
        |  ],
        |  "supportedLanguages": null
        |}
        |""".stripMargin
    val newDocument = """
        |{
        |  "tags": [
        |    {
        |      "tags": [
        |        "Brønn",
        |        "Brønnfag"
        |      ],
        |      "language": "nn"
        |    },
        |    {
        |      "tags": [
        |        "Brønn",
        |        "Brønnfag"
        |      ],
        |      "language": "nb"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "brønnhovudtrykk",
        |      "language": "nn"
        |    },
        |    {
        |      "title": "brønnhodetrykk",
        |      "language": "nb"
        |    }
        |  ],
        |  "status": {
        |    "other": [],
        |    "current": "PUBLISHED"
        |  },
        |  "content": [
        |    {
        |      "content": "Brønnhovudtrykk blir lese av på toppen av brønnen i overgangen frå brønnen til ventiltreet. Det er vanleg å bruke nemninga innestengt trykk i ein stengd brønn og strøymingstrykk i ein produserande brønn.\r\n\nBrønnhovudtrykket blir påverka av hydrostatisk trykk frå reservoaret til brønnhovudet, og strøymingstrykk gjennom produksjonsrøyret i brønnen og strupeventilen på overflata. Ein stengd gassbrønn kan ha eit brønnhovudtrykk som er veldig nært reservoartrykket, men ein stengd oljebrønn vil ha betydeleg lågare brønnhovudtrykk på grunn av den hydrostatiske trykkforskjellen frå reservoaret til toppen av brønnen.  \r\n\nPå engelsk heiter brønnhovudtrykk «wellhead pressure», og innestengt trykk heiter «shut in wellhead pressure». \r",
        |      "language": "nn"
        |    },
        |    {
        |      "content": "Brønnhodetrykk leses av på toppen av brønnen i overgangen fra brønnen til ventiltreet. Det er vanlig å bruke betegnelsene innestengt trykk i en stengt brønn og strømningstrykk i en produserende brønn.\r\n\nBrønnhodetrykket påvirkes av hydrostatisk trykk fra reservoaret til brønnhodet, og strømningstrykk gjennom produksjonsrøret i brønnen og strupeventilen på overflaten. En stengt gassbrønn kan ha et brønnhodetrykk som er veldig nært reservoartrykket, men en stengt oljebrønn vil ha betydelig lavere brønnhodetrykk på grunn av den hydrostatiske trykkforskjellen fra reservoaret til brønnens topp.  \r\n\nPå engelsk omtales brønnhodetrykk som «wellhead pressure», og innestengt trykk omtales som «shut in wellhead pressure». \r",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2018-07-02T10:59:12Z",
        |  "updated": "2021-06-28T06:29:22Z",
        |  "copyright": {
        |    "license": "CC-BY-SA-4.0",
        |    "creators": [
        |      {
        |        "name": "Sissel Paaske",
        |        "type": "writer"
        |      }
        |    ],
        |    "processed": false,
        |    "processors": [
        |      {
        |        "name": "Totaltekst",
        |        "type": "correction"
        |      }
        |    ],
        |    "rightsholders": []
        |  },
        |  "updatedBy": [
        |    "sPHJn0BEtfxw2d2DUpIuS3iY",
        |    "KBAJskRqPXZUv9LFjAbz8btB",
        |    "fFJdhmAbXvoHcph2vw2HfD_1",
        |    "4iOxWUcXV8La8_gC32Up9H0J",
        |    "yUT8Jlal2wT6AbLc0btQ1K5n"
        |  ],
        |  "conceptType": "concept",
        |  "editorNotes": [],
        |  "visualElement": [
        |    {
        |      "language": "nn",
        |      "visualElement": "<ndlaembed data-size=\"full\" data-align=\"\" data-caption=\"Brønnhodet til brønn A-20 på Sleipner plattformen.\" data-alt=\"Brønnhodet til brønn A-20 på Sleipner plattformen. Foto.\" data-resource_id=\"54741\" data-resource=\"image\"></ndlaembed>"
        |    },
        |    {
        |      "language": "nb",
        |      "visualElement": "<ndlaembed data-size=\"full\" data-align=\"\" data-caption=\"Brønnhodet til brønn A-20 på Sleipner plattformen.\" data-alt=\"Brønnhodet til brønn A-20 på Sleipner plattformen. Foto.\" data-resource_id=\"54741\" data-resource=\"image\"></ndlaembed>"
        |    }
        |  ],
        |  "supportedLanguages": null
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }
}
