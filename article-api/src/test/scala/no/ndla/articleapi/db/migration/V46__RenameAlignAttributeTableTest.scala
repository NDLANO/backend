/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V46__RenameAlignAttributeTableTest extends UnitSuite with TestEnvironment {

  class V45__MigrateMarkdownTest {
    test("That align is replaced as expected and valign is removed") {
      val oldDocument =
        """{  "id": 35036,  "revision": 46,  "title": {    "title": "Utregning av pris på brudepakke hos frisør",    "htmlTitle": "Utregning av pris på brudepakke hos frisør",    "language": "nb"  },  "content": {    "content": "<section><p> </p><ndlaembed data-resource=\"image\" data-resource_id=\"51158\" data-size=\"full\" data-align=\"\" data-alt=\"Vakker brud som ser på blomsterbuketten sin. Foto.\" data-caption=\"Hva kan en brudepakke hos frisør og makeupartist koste?\" data-url=\"https://api.test.ndla.no/image-api/v2/images/51158\"></ndlaembed><h2>Eksempel på utregning av pris på brudepakke</h2><p>En flott frisyre er viktig på den store dagen. En prøvetime gir frisøren mulighet til å bli kjent med kunden og håret hennes. Dere kan planlegge den frisyren som framhever kundens trekk og passer til brudekjolen.</p><ndlaembed data-resource=\"image\" data-resource_id=\"48590\" data-size=\"full\" data-align=\"\" data-alt=\"Krølling av hår på en kvinne hos en frisør. Både kunden og frisøren smiler. Foto.\" data-caption=\"God dialog og planlegging er viktig i en prøvetime.\" data-url=\"https://api.test.ndla.no/image-api/v2/images/48590\"></ndlaembed><div class=\"c-bodybox\"><h3>Begreper</h3><p><strong>Påslagstallet </strong>er tallet du ganger innkjøpspris med for å finne ut hva kunden må betale for et produkt. Dette tallet ligger som regel mellom 2,5 og 6.</p><p><strong>Merverdiavgift (mva.)</strong> er en avgift til staten på salg av produkter. I en påslagskalkyle er merverdiavgiften inkludert.</p></div><h2>Oppgave</h2><p>Sett opp en priskalkyle etter oppsettet nedenfor, der alt i brudepakken er inkludert.</p><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td valign="top" data-align="top"></td><td data-aling="bottom"><p> </p></td></tr></tbody></table><details><summary>Løsning</summary><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td align=\"right\"><p>kr 300</p></td><td align=\"right\"><p>kr 300</p></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td align=\"right\"><p>kr 120</p></td><td align=\"right\"><p>kr 120</p></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td align=\"right\"><p>kr 700</p></td><td align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td align=\"right\"><p>kr 700</p></td><td align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td align=\"right\"><p>kr 600</p></td><td align=\"right\"><p>kr 600</p></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 873</p></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 1 164</p></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 291</p></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td align=\"right\"><p> <strong>kr</strong> <strong>5 912</strong></p></td></tr></tbody></table><p>Pris for denne brudepakken er 6&#xa0;000 kroner (rundet opp).</p><p>Nedenfor kan du laste ned et regneark med utregningen over.</p><div data-type=\"file\"><ndlaembed data-path=\"/files/resources/luM8b5Z3ueGdJK2.xlsx\" data-type=\"xlsx\" data-title=\"Priskalkyle brudepakke\" data-resource=\"file\" data-url=\"https://api.test.ndla.no/files/resources/luM8b5Z3ueGdJK2.xlsx\"></ndlaembed></div></details><div data-type=\"related-content\"><ndlaembed data-resource=\"related-content\" data-article-id=\"32974\"></ndlaembed><ndlaembed data-resource=\"related-content\" data-article-id=\"33129\"></ndlaembed></div></section>",    "language": "nb"  },  "copyright": {    "license": {      "license": "CC-BY-SA-4.0",      "description": "Creative Commons Attribution-ShareAlike 4.0 International",      "url": "https://creativecommons.org/licenses/by-sa/4.0/"    },    "origin": "",    "creators": [      {        "type": "originator",        "name": "Hanne Merethe Riskedal Staurland"      }    ],    "processors": [      {        "type": "editorial",        "name": "Inger Gilje Sporaland"      },      {        "type": "editorial",        "name": "Bjarne Skurdal"      }    ],    "rightsholders": [],    "processed": false  },  "tags": {    "tags": [      "kalkulasjon",      "økonomi",      "brud",      "bryllup",      "frisør"    ],    "language": "nb"  },  "requiredLibraries": [],  "metaImage": {    "url": "https://api.test.ndla.no/image-api/raw/id/51643",    "alt": "Brudefrisyre. Foto.",    "language": "nb"  },  "introduction": {    "introduction": "Det er vanlig å tilby bruden en pakke bestående av prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlover. Dette er ofte en hyggelig stund for brud og forlover og inkluderer gjerne lette forfriskninger. Her er eksempel på utregning av pris.",    "htmlIntroduction": "Det er vanlig å tilby bruden en pakke bestående av prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlover. Dette er ofte en hyggelig stund for brud og forlover og inkluderer gjerne lette forfriskninger. Her er eksempel på utregning av pris.",    "language": "nb"  },  "metaDescription": {    "metaDescription": "Utregning av pris på arbeider til brud i frisørsalong.",    "language": "nb"  },  "created": "2022-01-22T20:15:36.000Z",  "updated": "2022-10-28T12:00:31.000Z",  "updatedBy": "455VO7UP9CLDSUU5TLugvgnc",  "published": "2022-01-22T20:15:36.000Z",  "articleType": "standard",  "supportedLanguages": [    "nb",    "nn"  ],  "grepCodes": [    "KE219",    "KM2503",    "KM368",    "KM369",    "KM343",    "KM344"  ],  "conceptIds": [],  "availability": "everyone",  "relatedContent": [],  "revisionDate": "2030-01-01T00:00:00.000Z"}"""
      val expectedDocument =
        """{  "id": 35036,  "revision": 46,  "title": {    "title": "Utregning av pris på brudepakke hos frisør",    "htmlTitle": "Utregning av pris på brudepakke hos frisør",    "language": "nb"  },  "content": {    "content": "<section><p> </p><ndlaembed data-resource=\"image\" data-resource_id=\"51158\" data-size=\"full\" data-align=\"\" data-alt=\"Vakker brud som ser på blomsterbuketten sin. Foto.\" data-caption=\"Hva kan en brudepakke hos frisør og makeupartist koste?\" data-url=\"https://api.test.ndla.no/image-api/v2/images/51158\"></ndlaembed><h2>Eksempel på utregning av pris på brudepakke</h2><p>En flott frisyre er viktig på den store dagen. En prøvetime gir frisøren mulighet til å bli kjent med kunden og håret hennes. Dere kan planlegge den frisyren som framhever kundens trekk og passer til brudekjolen.</p><ndlaembed data-resource=\"image\" data-resource_id=\"48590\" data-size=\"full\" data-align=\"\" data-alt=\"Krølling av hår på en kvinne hos en frisør. Både kunden og frisøren smiler. Foto.\" data-caption=\"God dialog og planlegging er viktig i en prøvetime.\" data-url=\"https://api.test.ndla.no/image-api/v2/images/48590\"></ndlaembed><div class=\"c-bodybox\"><h3>Begreper</h3><p><strong>Påslagstallet </strong>er tallet du ganger innkjøpspris med for å finne ut hva kunden må betale for et produkt. Dette tallet ligger som regel mellom 2,5 og 6.</p><p><strong>Merverdiavgift (mva.)</strong> er en avgift til staten på salg av produkter. I en påslagskalkyle er merverdiavgiften inkludert.</p></div><h2>Oppgave</h2><p>Sett opp en priskalkyle etter oppsettet nedenfor, der alt i brudepakken er inkludert.</p><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td><p> </p></td></tr></tbody></table><details><summary>Løsning</summary><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 300</p></td><td data-align=\"right\"><p>kr 300</p></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 120</p></td><td data-align=\"right\"><p>kr 120</p></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td data-align=\"right\"><p>kr 700</p></td><td data-align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td data-align=\"right\"><p>kr 700</p></td><td data-align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 600</p></td><td data-align=\"right\"><p>kr 600</p></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 873</p></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 1 164</p></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 291</p></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td data-align=\"right\"><p> <strong>kr</strong> <strong>5 912</strong></p></td></tr></tbody></table><p>Pris for denne brudepakken er 6&#xa0;000 kroner (rundet opp).</p><p>Nedenfor kan du laste ned et regneark med utregningen over.</p><div data-type=\"file\"><ndlaembed data-path=\"/files/resources/luM8b5Z3ueGdJK2.xlsx\" data-type=\"xlsx\" data-title=\"Priskalkyle brudepakke\" data-resource=\"file\" data-url=\"https://api.test.ndla.no/files/resources/luM8b5Z3ueGdJK2.xlsx\"></ndlaembed></div></details><div data-type=\"related-content\"><ndlaembed data-resource=\"related-content\" data-article-id=\"32974\"></ndlaembed><ndlaembed data-resource=\"related-content\" data-article-id=\"33129\"></ndlaembed></div></section>",    "language": "nb"  },  "copyright": {    "license": {      "license": "CC-BY-SA-4.0",      "description": "Creative Commons Attribution-ShareAlike 4.0 International",      "url": "https://creativecommons.org/licenses/by-sa/4.0/"    },    "origin": "",    "creators": [      {        "type": "originator",        "name": "Hanne Merethe Riskedal Staurland"      }    ],    "processors": [      {        "type": "editorial",        "name": "Inger Gilje Sporaland"      },      {        "type": "editorial",        "name": "Bjarne Skurdal"      }    ],    "rightsholders": [],    "processed": false  },  "tags": {    "tags": [      "kalkulasjon",      "økonomi",      "brud",      "bryllup",      "frisør"    ],    "language": "nb"  },  "requiredLibraries": [],  "metaImage": {    "url": "https://api.test.ndla.no/image-api/raw/id/51643",    "alt": "Brudefrisyre. Foto.",    "language": "nb"  },  "introduction": {    "introduction": "Det er vanlig å tilby bruden en pakke bestående av prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlover. Dette er ofte en hyggelig stund for brud og forlover og inkluderer gjerne lette forfriskninger. Her er eksempel på utregning av pris.",    "htmlIntroduction": "Det er vanlig å tilby bruden en pakke bestående av prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlover. Dette er ofte en hyggelig stund for brud og forlover og inkluderer gjerne lette forfriskninger. Her er eksempel på utregning av pris.",    "language": "nb"  },  "metaDescription": {    "metaDescription": "Utregning av pris på arbeider til brud i frisørsalong.",    "language": "nb"  },  "created": "2022-01-22T20:15:36.000Z",  "updated": "2022-10-28T12:00:31.000Z",  "updatedBy": "455VO7UP9CLDSUU5TLugvgnc",  "published": "2022-01-22T20:15:36.000Z",  "articleType": "standard",  "supportedLanguages": [    "nb",    "nn"  ],  "grepCodes": [    "KE219",    "KM2503",    "KM368",    "KM369",    "KM343",    "KM344"  ],  "conceptIds": [],  "availability": "everyone",  "relatedContent": [],  "revisionDate": "2030-01-01T00:00:00.000Z"}"""
      val migration = new V47__RenameAlignAttributeTable
      val result    = migration.convertDocument(oldDocument)
      result should be(expectedDocument)
    }
  }

}
