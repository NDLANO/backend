/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import io.circe.parser
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V69__FixContributorTypesTest extends UnitSuite with TestEnvironment {
  val migration = new V69__FixContributorTypes

  test("That copyright has correct contributor types") {
    val oldDocument = """
        |{
        |  "id": null,
        |  "slug": null,
        |  "tags": [
        |    {
        |      "tags": [
        |        "negative tall",
        |        "regnerekkefølge",
        |        "tallregning"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "regnerekkefølge",
        |        "talsystem"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "notes": [],
        |  "title": [
        |    {
        |      "title": "Tallregning",
        |      "language": "nb"
        |    },
        |    {
        |      "title": "Talrekning",
        |      "language": "nn"
        |    }
        |  ],
        |  "status": {
        |    "other": [
        |      "IMPORTED"
        |    ],
        |    "current": "PUBLISHED"
        |  },
        |  "content": [
        |    {
        |      "content": "<section><p>Tall er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god tallforståelse for å gjøre det bra i matematikk.</p><p>Tellestreker, hulemalerier og helleristninger viser at mennesker som levde for mange tusen år siden, brukte tall i sitt daglige liv. Arkeologer har funnet tellestreker som er over 30 000 år gamle. Strekene er systematisk risset inn og er antakelig blitt brukt under opptelling av gjenstander, dager eller andre objekter.</p><p>Vår sivilisasjon oppsto i Mesopotamia, landområdene mellom og rundt Eufrat og Tigris (nå Irak, nordøstlige Syria og sørøstlige Tyrkia), for ca. 5000 år siden. Her ble skrivekunsten oppfunnet. Menneskene som levde her, brukte <strong>kileskrift</strong>. De skrev på leirtavler og presset de kileformede tegnene inn i våt leire. På denne måten førte de blant annet regnskap over den handelen som utviklet seg mellom byene. Egypterne kjente til kileskriften, men utviklet sine egne skrifttegn, <strong>hieroglyfene</strong>. Utgravinger viser at det på denne tiden var mennesker som drev med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Senere laget både grekerne og romerne sine <strong>tallsystemer</strong>. Men det tallsystemet vi bruker i dag, med de ti symbolene 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har sin opprinnelse i India.  I de tidligste kulturene var tallet 0 og de negative tallene ikke kjent. Det var først på 1200-tallet at matematikere begynte å innføre disse tallene. Det tok likevel enda flere hundre år før de ble fullt ut akseptert. Matematikere diskuterte om negative tall virkelig eksisterte, og helt fram mot 1800-tallet var det matematikere som ikke ville akseptere beregninger som inneholdt negative tall.  Problemet med å forstå negative tall henger sammen med at tall ikke er noe konkret. Tall er abstrakte matematiske begreper. Vi må knytte tallene til noe konkret for å få en følelse av å forstå dem.</p><p>For oss som har vokst opp med bankvesen og gradestokk, er det lettere å forstå de negative tallene. Vi vet at vi kan komme til å bruke mer penger enn vi har på konto. På kontoutskriften fra banken står det da et tall med minus foran, og vi skjønner at vi står i gjeld til banken! Vi vet også at når det er kuldegrader ute, leser vi det av som negative tall på gradestokken. De negative tallene blir da konkrete, og vi føler at vi forstår dem.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "<section><p>Tal er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god talforståing for å gjere det bra i faget.</p><p>Teljestrekar, holemåleri og helleristingar syner at menneske som levde for mange tusen år sidan, brukte tal i dagleglivet. Arkeologar har funne teljestrekar som er over 30 000 år gamle. Strekane er systematisk rissa inn og er antakeleg blitt brukte under opptelling av gjenstandar, dagar eller andre objekt.</p><p>Sivilisasjonen vår oppstod i Mesopotamia, landområda mellom og rundt Eufrat og Tigris (no Irak, nordaustlege Syria og søraustlege Tyrkia), for ca. 5000 år sidan. Her blei skrivekunsten funnen opp. Menneska som levde der, brukte <strong>kileskrift</strong>. Dei skreiv på leirtavler og pressa dei kileforma teikna inn i våt leire. På denne måten førte dei mellom anna rekneskap over den handelen som utvikla seg mellom byane. Egyptarane kjende til kileskrifta, men utvikla sine eigne skriftteikn, <strong>hieroglyfane</strong>. Utgravingar syner at det på denne tida var menneske som dreiv med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Seinare laga både grekarane og romarane sine <strong>talsystem</strong>. Men det talsystemet vi bruker i dag, med dei ti symbola 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har opphav i India.  I dei tidlegaste kulturane var talet 0 og dei negative tala ikkje kjende. Det var først på 1200-talet at matematikarar byrja å innføre desse tala. Det tok likevel endå fleire hundre år før dei vart fullt ut aksepterte. Matematikarar diskuterte om negative tal verkeleg eksisterte, og heilt fram mot 1800-talet var det matematikarar som ikkje ville akseptere utrekningar som inneheldt negative tal.  Problemet med å forstå negative tal heng saman med at tal ikkje er noko konkret. Tal er abstrakte matematiske omgrep. Vi må knyte tala til noko konkret for å få ei kjensle av å forstå dei.</p><p>For oss som har vakse opp med bankvesen og gradestokk, er det lettare å forstå dei negative tala. Vi veit at om vi kan komme til å bruke meir pengar enn vi har på konto. På kontoutskrifta frå banken står det då eit tal med minus framfor, og vi skjønner at vi står i gjeld til banken! Vi veit også at når det er kuldegradar ute, les vi det av som negative tal på gradestokken. Dei negative tala blir då konkrete, og vi kjenner at vi forstår dei.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nn"
        |    }
        |  ],
        |  "created": "2018-02-13T17:54:22.000Z",
        |  "started": false,
        |  "updated": "2018-02-13T17:56:35.000Z",
        |  "comments": [],
        |  "priority": "unspecified",
        |  "revision": null,
        |  "copyright": {
        |    "origin": null,
        |    "license": "CC-BY-NC-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "Forfatter",
        |        "name": "Olav Kristensen"
        |      },
        |      {
        |        "type": "Bearbeider",
        |        "name": "Stein Aanensen"
        |      }
        |    ],
        |    "processors": [
        |      {
        |        "type": "Editorial",
        |        "name": "Elisabet Romedal"
        |      }
        |    ],
        |    "rightsholders": []
        |  },
        |  "grepCodes": [],
        |  "metaImage": [
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nb"
        |    },
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nn"
        |    }
        |  ],
        |  "published": "2018-02-13T17:56:35.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "topic-article",
        |  "responsible": null,
        |  "availability": "everyone",
        |  "editorLabels": [],
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "I temaet tallregning skal vi vurdere, velge og bruke matematiske metoder og verktøy til å løse problemer fra ulike fag og samfunnsområder, og reflektere over, vurdere og presentere løsningene på en hensiktsmessig måte."
        |    },
        |    {
        |      "language": "nn",
        |      "introduction": "I temaet talrekning skal vi vurdere, velje og bruke matematiske metodar og verktøy til å løyse problem frå ulike fag og samfunnsområde, og reflektere over, vurdere og presentere løysingane på ein føremålstenleg måte."
        |    }
        |  ],
        |  "revisionMeta": [
        |    {
        |      "id": "0b82b045-f1e4-48af-b122-68f63f86e274",
        |      "note": "Automatisk revisjonsdato satt av systemet.",
        |      "status": "needs-revision",
        |      "revisionDate": "2030-01-01T00:00:00.000Z"
        |    }
        |  ],
        |  "visualElement": [
        |    {
        |      "language": "nb",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    },
        |    {
        |      "language": "nn",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    }
        |  ],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Tallregning handler om å behandle tall i praktisk regning, bruke regningsartene, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "Tallrekning handlar om å handtere tal i praktisk rekning, bruke rekningsartane, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nn"
        |    }
        |  ],
        |  "qualityEvaluation": null,
        |  "requiredLibraries": [],
        |  "previousVersionsNotes": [
        |    {
        |      "note": "Status endret",
        |      "user": "nd3KiThoNIQ0KX5flWoFxUqr",
        |      "status": {
        |        "other": [
        |          "IMPORTED"
        |        ],
        |        "current": "PUBLISHED"
        |      },
        |      "timestamp": "2020-06-08T14:02:49.000Z"
        |    }
        |  ]
        |}
        |""".stripMargin
    val newDocument = """
        |{
        |  "id": null,
        |  "slug": null,
        |  "tags": [
        |    {
        |      "tags": [
        |        "negative tall",
        |        "regnerekkefølge",
        |        "tallregning"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "regnerekkefølge",
        |        "talsystem"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "notes": [],
        |  "title": [
        |    {
        |      "title": "Tallregning",
        |      "language": "nb"
        |    },
        |    {
        |      "title": "Talrekning",
        |      "language": "nn"
        |    }
        |  ],
        |  "status": {
        |    "other": [
        |      "IMPORTED"
        |    ],
        |    "current": "PUBLISHED"
        |  },
        |  "content": [
        |    {
        |      "content": "<section><p>Tall er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god tallforståelse for å gjøre det bra i matematikk.</p><p>Tellestreker, hulemalerier og helleristninger viser at mennesker som levde for mange tusen år siden, brukte tall i sitt daglige liv. Arkeologer har funnet tellestreker som er over 30 000 år gamle. Strekene er systematisk risset inn og er antakelig blitt brukt under opptelling av gjenstander, dager eller andre objekter.</p><p>Vår sivilisasjon oppsto i Mesopotamia, landområdene mellom og rundt Eufrat og Tigris (nå Irak, nordøstlige Syria og sørøstlige Tyrkia), for ca. 5000 år siden. Her ble skrivekunsten oppfunnet. Menneskene som levde her, brukte <strong>kileskrift</strong>. De skrev på leirtavler og presset de kileformede tegnene inn i våt leire. På denne måten førte de blant annet regnskap over den handelen som utviklet seg mellom byene. Egypterne kjente til kileskriften, men utviklet sine egne skrifttegn, <strong>hieroglyfene</strong>. Utgravinger viser at det på denne tiden var mennesker som drev med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Senere laget både grekerne og romerne sine <strong>tallsystemer</strong>. Men det tallsystemet vi bruker i dag, med de ti symbolene 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har sin opprinnelse i India.  I de tidligste kulturene var tallet 0 og de negative tallene ikke kjent. Det var først på 1200-tallet at matematikere begynte å innføre disse tallene. Det tok likevel enda flere hundre år før de ble fullt ut akseptert. Matematikere diskuterte om negative tall virkelig eksisterte, og helt fram mot 1800-tallet var det matematikere som ikke ville akseptere beregninger som inneholdt negative tall.  Problemet med å forstå negative tall henger sammen med at tall ikke er noe konkret. Tall er abstrakte matematiske begreper. Vi må knytte tallene til noe konkret for å få en følelse av å forstå dem.</p><p>For oss som har vokst opp med bankvesen og gradestokk, er det lettere å forstå de negative tallene. Vi vet at vi kan komme til å bruke mer penger enn vi har på konto. På kontoutskriften fra banken står det da et tall med minus foran, og vi skjønner at vi står i gjeld til banken! Vi vet også at når det er kuldegrader ute, leser vi det av som negative tall på gradestokken. De negative tallene blir da konkrete, og vi føler at vi forstår dem.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "<section><p>Tal er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god talforståing for å gjere det bra i faget.</p><p>Teljestrekar, holemåleri og helleristingar syner at menneske som levde for mange tusen år sidan, brukte tal i dagleglivet. Arkeologar har funne teljestrekar som er over 30 000 år gamle. Strekane er systematisk rissa inn og er antakeleg blitt brukte under opptelling av gjenstandar, dagar eller andre objekt.</p><p>Sivilisasjonen vår oppstod i Mesopotamia, landområda mellom og rundt Eufrat og Tigris (no Irak, nordaustlege Syria og søraustlege Tyrkia), for ca. 5000 år sidan. Her blei skrivekunsten funnen opp. Menneska som levde der, brukte <strong>kileskrift</strong>. Dei skreiv på leirtavler og pressa dei kileforma teikna inn i våt leire. På denne måten førte dei mellom anna rekneskap over den handelen som utvikla seg mellom byane. Egyptarane kjende til kileskrifta, men utvikla sine eigne skriftteikn, <strong>hieroglyfane</strong>. Utgravingar syner at det på denne tida var menneske som dreiv med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Seinare laga både grekarane og romarane sine <strong>talsystem</strong>. Men det talsystemet vi bruker i dag, med dei ti symbola 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har opphav i India.  I dei tidlegaste kulturane var talet 0 og dei negative tala ikkje kjende. Det var først på 1200-talet at matematikarar byrja å innføre desse tala. Det tok likevel endå fleire hundre år før dei vart fullt ut aksepterte. Matematikarar diskuterte om negative tal verkeleg eksisterte, og heilt fram mot 1800-talet var det matematikarar som ikkje ville akseptere utrekningar som inneheldt negative tal.  Problemet med å forstå negative tal heng saman med at tal ikkje er noko konkret. Tal er abstrakte matematiske omgrep. Vi må knyte tala til noko konkret for å få ei kjensle av å forstå dei.</p><p>For oss som har vakse opp med bankvesen og gradestokk, er det lettare å forstå dei negative tala. Vi veit at om vi kan komme til å bruke meir pengar enn vi har på konto. På kontoutskrifta frå banken står det då eit tal med minus framfor, og vi skjønner at vi står i gjeld til banken! Vi veit også at når det er kuldegradar ute, les vi det av som negative tal på gradestokken. Dei negative tala blir då konkrete, og vi kjenner at vi forstår dei.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nn"
        |    }
        |  ],
        |  "created": "2018-02-13T17:54:22.000Z",
        |  "started": false,
        |  "updated": "2018-02-13T17:56:35.000Z",
        |  "comments": [],
        |  "priority": "unspecified",
        |  "revision": null,
        |  "copyright": {
        |    "origin": null,
        |    "license": "CC-BY-NC-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "writer",
        |        "name": "Olav Kristensen"
        |      },
        |      {
        |        "type": "processor",
        |        "name": "Stein Aanensen"
        |      }
        |    ],
        |    "processors": [
        |      {
        |        "type": "editorial",
        |        "name": "Elisabet Romedal"
        |      }
        |    ],
        |    "rightsholders": []
        |  },
        |  "grepCodes": [],
        |  "metaImage": [
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nb"
        |    },
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nn"
        |    }
        |  ],
        |  "published": "2018-02-13T17:56:35.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "topic-article",
        |  "responsible": null,
        |  "availability": "everyone",
        |  "editorLabels": [],
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "I temaet tallregning skal vi vurdere, velge og bruke matematiske metoder og verktøy til å løse problemer fra ulike fag og samfunnsområder, og reflektere over, vurdere og presentere løsningene på en hensiktsmessig måte."
        |    },
        |    {
        |      "language": "nn",
        |      "introduction": "I temaet talrekning skal vi vurdere, velje og bruke matematiske metodar og verktøy til å løyse problem frå ulike fag og samfunnsområde, og reflektere over, vurdere og presentere løysingane på ein føremålstenleg måte."
        |    }
        |  ],
        |  "revisionMeta": [
        |    {
        |      "id": "0b82b045-f1e4-48af-b122-68f63f86e274",
        |      "note": "Automatisk revisjonsdato satt av systemet.",
        |      "status": "needs-revision",
        |      "revisionDate": "2030-01-01T00:00:00.000Z"
        |    }
        |  ],
        |  "visualElement": [
        |    {
        |      "language": "nb",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    },
        |    {
        |      "language": "nn",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    }
        |  ],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Tallregning handler om å behandle tall i praktisk regning, bruke regningsartene, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "Tallrekning handlar om å handtere tal i praktisk rekning, bruke rekningsartane, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nn"
        |    }
        |  ],
        |  "qualityEvaluation": null,
        |  "requiredLibraries": [],
        |  "previousVersionsNotes": [
        |    {
        |      "note": "Status endret",
        |      "user": "nd3KiThoNIQ0KX5flWoFxUqr",
        |      "status": {
        |        "other": [
        |          "IMPORTED"
        |        ],
        |        "current": "PUBLISHED"
        |      },
        |      "timestamp": "2020-06-08T14:02:49.000Z"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

  test("That migration handles broken copyright data") {
    val oldDocument = """
        |{
        |  "id": null,
        |  "slug": null,
        |  "tags": [
        |    {
        |      "tags": [
        |        "negative tall",
        |        "regnerekkefølge",
        |        "tallregning"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "regnerekkefølge",
        |        "talsystem"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "notes": [],
        |  "title": [
        |    {
        |      "title": "Tallregning",
        |      "language": "nb"
        |    },
        |    {
        |      "title": "Talrekning",
        |      "language": "nn"
        |    }
        |  ],
        |  "status": {
        |    "other": [
        |      "IMPORTED"
        |    ],
        |    "current": "PUBLISHED"
        |  },
        |  "content": [
        |    {
        |      "content": "<section><p>Tall er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god tallforståelse for å gjøre det bra i matematikk.</p><p>Tellestreker, hulemalerier og helleristninger viser at mennesker som levde for mange tusen år siden, brukte tall i sitt daglige liv. Arkeologer har funnet tellestreker som er over 30 000 år gamle. Strekene er systematisk risset inn og er antakelig blitt brukt under opptelling av gjenstander, dager eller andre objekter.</p><p>Vår sivilisasjon oppsto i Mesopotamia, landområdene mellom og rundt Eufrat og Tigris (nå Irak, nordøstlige Syria og sørøstlige Tyrkia), for ca. 5000 år siden. Her ble skrivekunsten oppfunnet. Menneskene som levde her, brukte <strong>kileskrift</strong>. De skrev på leirtavler og presset de kileformede tegnene inn i våt leire. På denne måten førte de blant annet regnskap over den handelen som utviklet seg mellom byene. Egypterne kjente til kileskriften, men utviklet sine egne skrifttegn, <strong>hieroglyfene</strong>. Utgravinger viser at det på denne tiden var mennesker som drev med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Senere laget både grekerne og romerne sine <strong>tallsystemer</strong>. Men det tallsystemet vi bruker i dag, med de ti symbolene 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har sin opprinnelse i India.  I de tidligste kulturene var tallet 0 og de negative tallene ikke kjent. Det var først på 1200-tallet at matematikere begynte å innføre disse tallene. Det tok likevel enda flere hundre år før de ble fullt ut akseptert. Matematikere diskuterte om negative tall virkelig eksisterte, og helt fram mot 1800-tallet var det matematikere som ikke ville akseptere beregninger som inneholdt negative tall.  Problemet med å forstå negative tall henger sammen med at tall ikke er noe konkret. Tall er abstrakte matematiske begreper. Vi må knytte tallene til noe konkret for å få en følelse av å forstå dem.</p><p>For oss som har vokst opp med bankvesen og gradestokk, er det lettere å forstå de negative tallene. Vi vet at vi kan komme til å bruke mer penger enn vi har på konto. På kontoutskriften fra banken står det da et tall med minus foran, og vi skjønner at vi står i gjeld til banken! Vi vet også at når det er kuldegrader ute, leser vi det av som negative tall på gradestokken. De negative tallene blir da konkrete, og vi føler at vi forstår dem.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "<section><p>Tal er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god talforståing for å gjere det bra i faget.</p><p>Teljestrekar, holemåleri og helleristingar syner at menneske som levde for mange tusen år sidan, brukte tal i dagleglivet. Arkeologar har funne teljestrekar som er over 30 000 år gamle. Strekane er systematisk rissa inn og er antakeleg blitt brukte under opptelling av gjenstandar, dagar eller andre objekt.</p><p>Sivilisasjonen vår oppstod i Mesopotamia, landområda mellom og rundt Eufrat og Tigris (no Irak, nordaustlege Syria og søraustlege Tyrkia), for ca. 5000 år sidan. Her blei skrivekunsten funnen opp. Menneska som levde der, brukte <strong>kileskrift</strong>. Dei skreiv på leirtavler og pressa dei kileforma teikna inn i våt leire. På denne måten førte dei mellom anna rekneskap over den handelen som utvikla seg mellom byane. Egyptarane kjende til kileskrifta, men utvikla sine eigne skriftteikn, <strong>hieroglyfane</strong>. Utgravingar syner at det på denne tida var menneske som dreiv med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Seinare laga både grekarane og romarane sine <strong>talsystem</strong>. Men det talsystemet vi bruker i dag, med dei ti symbola 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har opphav i India.  I dei tidlegaste kulturane var talet 0 og dei negative tala ikkje kjende. Det var først på 1200-talet at matematikarar byrja å innføre desse tala. Det tok likevel endå fleire hundre år før dei vart fullt ut aksepterte. Matematikarar diskuterte om negative tal verkeleg eksisterte, og heilt fram mot 1800-talet var det matematikarar som ikkje ville akseptere utrekningar som inneheldt negative tal.  Problemet med å forstå negative tal heng saman med at tal ikkje er noko konkret. Tal er abstrakte matematiske omgrep. Vi må knyte tala til noko konkret for å få ei kjensle av å forstå dei.</p><p>For oss som har vakse opp med bankvesen og gradestokk, er det lettare å forstå dei negative tala. Vi veit at om vi kan komme til å bruke meir pengar enn vi har på konto. På kontoutskrifta frå banken står det då eit tal med minus framfor, og vi skjønner at vi står i gjeld til banken! Vi veit også at når det er kuldegradar ute, les vi det av som negative tal på gradestokken. Dei negative tala blir då konkrete, og vi kjenner at vi forstår dei.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nn"
        |    }
        |  ],
        |  "created": "2018-02-13T17:54:22.000Z",
        |  "started": false,
        |  "updated": "2018-02-13T17:56:35.000Z",
        |  "comments": [],
        |  "priority": "unspecified",
        |  "revision": null,
        |  "copyright": null,
        |  "grepCodes": [],
        |  "metaImage": [
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nb"
        |    },
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nn"
        |    }
        |  ],
        |  "published": "2018-02-13T17:56:35.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "topic-article",
        |  "responsible": null,
        |  "availability": "everyone",
        |  "editorLabels": [],
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "I temaet tallregning skal vi vurdere, velge og bruke matematiske metoder og verktøy til å løse problemer fra ulike fag og samfunnsområder, og reflektere over, vurdere og presentere løsningene på en hensiktsmessig måte."
        |    },
        |    {
        |      "language": "nn",
        |      "introduction": "I temaet talrekning skal vi vurdere, velje og bruke matematiske metodar og verktøy til å løyse problem frå ulike fag og samfunnsområde, og reflektere over, vurdere og presentere løysingane på ein føremålstenleg måte."
        |    }
        |  ],
        |  "revisionMeta": [
        |    {
        |      "id": "0b82b045-f1e4-48af-b122-68f63f86e274",
        |      "note": "Automatisk revisjonsdato satt av systemet.",
        |      "status": "needs-revision",
        |      "revisionDate": "2030-01-01T00:00:00.000Z"
        |    }
        |  ],
        |  "visualElement": [
        |    {
        |      "language": "nb",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    },
        |    {
        |      "language": "nn",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    }
        |  ],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Tallregning handler om å behandle tall i praktisk regning, bruke regningsartene, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "Tallrekning handlar om å handtere tal i praktisk rekning, bruke rekningsartane, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nn"
        |    }
        |  ],
        |  "qualityEvaluation": null,
        |  "requiredLibraries": [],
        |  "previousVersionsNotes": [
        |    {
        |      "note": "Status endret",
        |      "user": "nd3KiThoNIQ0KX5flWoFxUqr",
        |      "status": {
        |        "other": [
        |          "IMPORTED"
        |        ],
        |        "current": "PUBLISHED"
        |      },
        |      "timestamp": "2020-06-08T14:02:49.000Z"
        |    }
        |  ]
        |}
        |""".stripMargin
    val newDocument = """
        |{
        |  "id": null,
        |  "slug": null,
        |  "tags": [
        |    {
        |      "tags": [
        |        "negative tall",
        |        "regnerekkefølge",
        |        "tallregning"
        |      ],
        |      "language": "nb"
        |    },
        |    {
        |      "tags": [
        |        "regnerekkefølge",
        |        "talsystem"
        |      ],
        |      "language": "nn"
        |    }
        |  ],
        |  "notes": [],
        |  "title": [
        |    {
        |      "title": "Tallregning",
        |      "language": "nb"
        |    },
        |    {
        |      "title": "Talrekning",
        |      "language": "nn"
        |    }
        |  ],
        |  "status": {
        |    "other": [
        |      "IMPORTED"
        |    ],
        |    "current": "PUBLISHED"
        |  },
        |  "content": [
        |    {
        |      "content": "<section><p>Tall er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god tallforståelse for å gjøre det bra i matematikk.</p><p>Tellestreker, hulemalerier og helleristninger viser at mennesker som levde for mange tusen år siden, brukte tall i sitt daglige liv. Arkeologer har funnet tellestreker som er over 30 000 år gamle. Strekene er systematisk risset inn og er antakelig blitt brukt under opptelling av gjenstander, dager eller andre objekter.</p><p>Vår sivilisasjon oppsto i Mesopotamia, landområdene mellom og rundt Eufrat og Tigris (nå Irak, nordøstlige Syria og sørøstlige Tyrkia), for ca. 5000 år siden. Her ble skrivekunsten oppfunnet. Menneskene som levde her, brukte <strong>kileskrift</strong>. De skrev på leirtavler og presset de kileformede tegnene inn i våt leire. På denne måten førte de blant annet regnskap over den handelen som utviklet seg mellom byene. Egypterne kjente til kileskriften, men utviklet sine egne skrifttegn, <strong>hieroglyfene</strong>. Utgravinger viser at det på denne tiden var mennesker som drev med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Senere laget både grekerne og romerne sine <strong>tallsystemer</strong>. Men det tallsystemet vi bruker i dag, med de ti symbolene 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har sin opprinnelse i India.  I de tidligste kulturene var tallet 0 og de negative tallene ikke kjent. Det var først på 1200-tallet at matematikere begynte å innføre disse tallene. Det tok likevel enda flere hundre år før de ble fullt ut akseptert. Matematikere diskuterte om negative tall virkelig eksisterte, og helt fram mot 1800-tallet var det matematikere som ikke ville akseptere beregninger som inneholdt negative tall.  Problemet med å forstå negative tall henger sammen med at tall ikke er noe konkret. Tall er abstrakte matematiske begreper. Vi må knytte tallene til noe konkret for å få en følelse av å forstå dem.</p><p>For oss som har vokst opp med bankvesen og gradestokk, er det lettere å forstå de negative tallene. Vi vet at vi kan komme til å bruke mer penger enn vi har på konto. På kontoutskriften fra banken står det da et tall med minus foran, og vi skjønner at vi står i gjeld til banken! Vi vet også at når det er kuldegrader ute, leser vi det av som negative tall på gradestokken. De negative tallene blir da konkrete, og vi føler at vi forstår dem.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "<section><p>Tal er grunnlaget for all matematikk. Det er derfor veldig viktig å ha god talforståing for å gjere det bra i faget.</p><p>Teljestrekar, holemåleri og helleristingar syner at menneske som levde for mange tusen år sidan, brukte tal i dagleglivet. Arkeologar har funne teljestrekar som er over 30 000 år gamle. Strekane er systematisk rissa inn og er antakeleg blitt brukte under opptelling av gjenstandar, dagar eller andre objekt.</p><p>Sivilisasjonen vår oppstod i Mesopotamia, landområda mellom og rundt Eufrat og Tigris (no Irak, nordaustlege Syria og søraustlege Tyrkia), for ca. 5000 år sidan. Her blei skrivekunsten funnen opp. Menneska som levde der, brukte <strong>kileskrift</strong>. Dei skreiv på leirtavler og pressa dei kileforma teikna inn i våt leire. På denne måten førte dei mellom anna rekneskap over den handelen som utvikla seg mellom byane. Egyptarane kjende til kileskrifta, men utvikla sine eigne skriftteikn, <strong>hieroglyfane</strong>. Utgravingar syner at det på denne tida var menneske som dreiv med <strong>addisjon</strong>,<strong> subtraksjon</strong>,<strong> multiplikasjon </strong>og <strong>divisjon</strong>.</p><p>Seinare laga både grekarane og romarane sine <strong>talsystem</strong>. Men det talsystemet vi bruker i dag, med dei ti symbola 0, 1, 2, 3, 4, 5, 6, 7, 8 og 9, har opphav i India.  I dei tidlegaste kulturane var talet 0 og dei negative tala ikkje kjende. Det var først på 1200-talet at matematikarar byrja å innføre desse tala. Det tok likevel endå fleire hundre år før dei vart fullt ut aksepterte. Matematikarar diskuterte om negative tal verkeleg eksisterte, og heilt fram mot 1800-talet var det matematikarar som ikkje ville akseptere utrekningar som inneheldt negative tal.  Problemet med å forstå negative tal heng saman med at tal ikkje er noko konkret. Tal er abstrakte matematiske omgrep. Vi må knyte tala til noko konkret for å få ei kjensle av å forstå dei.</p><p>For oss som har vakse opp med bankvesen og gradestokk, er det lettare å forstå dei negative tala. Vi veit at om vi kan komme til å bruke meir pengar enn vi har på konto. På kontoutskrifta frå banken står det då eit tal med minus framfor, og vi skjønner at vi står i gjeld til banken! Vi veit også at når det er kuldegradar ute, les vi det av som negative tal på gradestokken. Dei negative tala blir då konkrete, og vi kjenner at vi forstår dei.</p></section><section><div data-type=\"related-content\"><ndlaembed data-article-id=\"7360\" data-resource=\"related-content\"></ndlaembed></div></section>",
        |      "language": "nn"
        |    }
        |  ],
        |  "created": "2018-02-13T17:54:22.000Z",
        |  "started": false,
        |  "updated": "2018-02-13T17:56:35.000Z",
        |  "comments": [],
        |  "priority": "unspecified",
        |  "revision": null,
        |  "copyright": null,
        |  "grepCodes": [],
        |  "metaImage": [
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nb"
        |    },
        |    {
        |      "altText": "Bannerbilde for temaet tall og algebra i faget 1T. Bilde.",
        |      "imageId": "1519",
        |      "language": "nn"
        |    }
        |  ],
        |  "published": "2018-02-13T17:56:35.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "topic-article",
        |  "responsible": null,
        |  "availability": "everyone",
        |  "editorLabels": [],
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "I temaet tallregning skal vi vurdere, velge og bruke matematiske metoder og verktøy til å løse problemer fra ulike fag og samfunnsområder, og reflektere over, vurdere og presentere løsningene på en hensiktsmessig måte."
        |    },
        |    {
        |      "language": "nn",
        |      "introduction": "I temaet talrekning skal vi vurdere, velje og bruke matematiske metodar og verktøy til å løyse problem frå ulike fag og samfunnsområde, og reflektere over, vurdere og presentere løysingane på ein føremålstenleg måte."
        |    }
        |  ],
        |  "revisionMeta": [
        |    {
        |      "id": "0b82b045-f1e4-48af-b122-68f63f86e274",
        |      "note": "Automatisk revisjonsdato satt av systemet.",
        |      "status": "needs-revision",
        |      "revisionDate": "2030-01-01T00:00:00.000Z"
        |    }
        |  ],
        |  "visualElement": [
        |    {
        |      "language": "nb",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    },
        |    {
        |      "language": "nn",
        |      "resource": "<ndlaembed data-align=\"\" data-alt=\"\" data-caption=\"\" data-resource=\"image\" data-resource_id=\"39446\" data-size=\"\"></ndlaembed>"
        |    }
        |  ],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Tallregning handler om å behandle tall i praktisk regning, bruke regningsartene, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nb"
        |    },
        |    {
        |      "content": "Tallrekning handlar om å handtere tal i praktisk rekning, bruke rekningsartane, addisjon, subtraksjon, divisjon og multiplikasjon.",
        |      "language": "nn"
        |    }
        |  ],
        |  "qualityEvaluation": null,
        |  "requiredLibraries": [],
        |  "previousVersionsNotes": [
        |    {
        |      "note": "Status endret",
        |      "user": "nd3KiThoNIQ0KX5flWoFxUqr",
        |      "status": {
        |        "other": [
        |          "IMPORTED"
        |        ],
        |        "current": "PUBLISHED"
        |      },
        |      "timestamp": "2020-06-08T14:02:49.000Z"
        |    }
        |  ]
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(oldDocument) should be(expected.noSpaces)
  }

}
