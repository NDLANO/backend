/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}
class V58__RenameAlignAttributeTableTest extends UnitSuite with TestEnvironment {

  class V45__MigrateMarkdownTest {

    test("That markdown is migrated correctly") {
      val oldDocument =
        """{"tags": [{"tags": ["kalkulasjon", "økonomi", "brud", "bryllup", "frisør"], "language": "nb"}, {"tags": ["kalkulasjon", "økonomi", "brud", "bryllaup", "frisør"], "language": "nn"}], "notes": [{"note": "Oppdatert taksonomi.", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "PUBLISHED"}, "timestamp": "2022-11-25T08:22:21Z"}], "title": [{"title": "Utregning av pris på brudepakke hos frisør", "language": "nb"}, {"title": "Utrekning av pris på brudepakke hos frisør", "language": "nn"}], "status": {"other": [], "current": "PUBLISHED"}, "content": [{"content": "<section><p> </p><ndlaembed data-resource=\"image\" data-resource_id=\"51158\" data-size=\"full\" data-align=\"\" data-alt=\"Vakker brud som ser på blomsterbuketten sin. Foto.\" data-caption=\"Hva kan en brudepakke hos frisør og makeupartist koste?\"></ndlaembed><h2>Eksempel på utregning av pris på brudepakke</h2><p>En flott frisyre er viktig på den store dagen. En prøvetime gir frisøren mulighet til å bli kjent med kunden og håret hennes. Dere kan planlegge den frisyren som framhever kundens trekk og passer til brudekjolen.</p><ndlaembed data-resource=\"image\" data-resource_id=\"48590\" data-size=\"full\" data-align=\"\" data-alt=\"Krølling av hår på en kvinne hos en frisør. Både kunden og frisøren smiler. Foto.\" data-caption=\"God dialog og planlegging er viktig i en prøvetime.\"></ndlaembed><div data-type=\"framed-content\"><h3>Begreper</h3><p><strong>Påslagstallet </strong>er tallet du ganger innkjøpspris med for å finne ut hva kunden må betale for et produkt. Dette tallet ligger som regel mellom 2,5 og 6.</p><p><strong>Merverdiavgift (mva.)</strong> er en avgift til staten på salg av produkter. I en påslagskalkyle er merverdiavgiften inkludert.</p></div><h2>Oppgave</h2><p>Sett opp en priskalkyle etter oppsettet nedenfor, der alt i brudepakken er inkludert.</p><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td><p> </p></td></tr></tbody></table><details><summary>Løsning</summary><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td align=\"right\"><p>kr 300</p></td><td align=\"right\"><p>kr 300</p></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td align=\"right\"><p>kr 120</p></td><td align=\"right\"><p>kr 120</p></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td align=\"right\"><p>kr 700</p></td><td align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td align=\"right\"><p>kr 700</p></td><td align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td align=\"right\"><p>kr 600</p></td><td align=\"right\"><p>kr 600</p></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td valign="top" align=\"right\"><p>kr 873</p></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td valign="top"><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 1 164</p></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 291</p></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td align=\"right\"><p> <strong>kr</strong> <strong>5 912</strong></p></td></tr></tbody></table><p>Pris for denne brudepakken er 6&#xa0;000 kroner (rundet opp).</p><p>Nedenfor kan du laste ned et regneark med utregningen over.</p><div data-type=\"file\"><ndlaembed data-path=\"/files/resources/luM8b5Z3ueGdJK2.xlsx\" data-type=\"xlsx\" data-title=\"Priskalkyle brudepakke\" data-resource=\"file\"></ndlaembed></div></details><div data-type=\"related-content\"><ndlaembed data-resource=\"related-content\" data-article-id=\"32974\"></ndlaembed><ndlaembed data-resource=\"related-content\" data-article-id=\"33129\"></ndlaembed></div></section>", "language": "nb"}, {"content": "<section><p> </p><ndlaembed data-resource=\"image\" data-resource_id=\"51158\" data-size=\"full\" data-align=\"\" data-alt=\"Vakker brud som ser på blomsterbuketten sin. Foto.\" data-caption=\"Kva kan ein brudepakke hos frisør og makeupartist koste?\"></ndlaembed><h2>Døme på utrekning av pris på brudepakke</h2><p>Ein flott frisyre er viktig på den store dagen. Ein prøvetime gir frisøren moglegheit til å bli kjend med kunden og håret hennar. De kan planlegge den frisyren som framhevar trekka til kunden og passar til brudekjolen.</p><ndlaembed data-resource=\"image\" data-resource_id=\"48590\" data-size=\"full\" data-align=\"\" data-alt=\"Krølling av hår på ei kvinne hos ein frisør. Både kunden og frisøren smiler. Foto.\" data-caption=\"God dialog og planlegging er viktig i ein prøvetime.\"></ndlaembed><div data-type=\"framed-content\"><h3>Omgrep</h3><p><strong>Påslagstalet </strong>er talet du gonger innkjøpspris med for å finne ut kva kunden må betale for eit produkt. Dette talet ligg som regel mellom 2,5 og 6.</p><p><strong>Meirverdiavgift (mva.)</strong> er ei avgift til staten på sal av produkt. I ein påslagskalkyle er meirverdiavgifta inkludert.</p></div><h2>Oppgåve</h2><p>Set opp ein priskalkyle etter oppsettet nedanfor, der alt i brudepakken er inkludert.</p><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Skildring</p></th><th scope=\"col\"><p>Tal</p></th><th scope=\"col\"><p>Innkjøpspris per stk. utan mva.</p></th><th scope=\"col\"><p>Påslags-tal</p></th><th scope=\"col\"><p>Utsalspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkt til hår</p></th><td><p>1</p></td><td align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffi og konfekt</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspengar</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspengar</p></th><td><p>2</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Styling forlovar</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeup forlovar</p></th><td><p>0,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td><p> </p></td></tr></tbody></table><details><summary>Løysing</summary><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Skildring</p></th><th scope=\"col\"><p>Tal</p></th><th scope=\"col\"><p>Innkjøpspris per stk. utan mva.</p></th><th scope=\"col\"><p>Påslags-tal</p></th><th scope=\"col\"><p>Utsalspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkt til hår</p></th><td><p>1</p></td><td align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td align=\"right\"><p>kr 300</p></td><td align=\"right\"><p>kr 300</p></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td align=\"right\"><p>kr 120</p></td><td align=\"right\"><p>kr 120</p></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffi og konfekt</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td align=\"right\"><p>kr 700</p></td><td align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td align=\"right\"><p>kr 700</p></td><td align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td align=\"right\"><p>kr 600</p></td><td align=\"right\"><p>kr 600</p></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 873</p></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspengar</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspengar</p></th><td><p>2</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 1 164</p></td></tr><tr><th scope=\"row\"><p>Styling forlovar</p></th><td><p>1</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Makeup forlovar</p></th><td><p>0,5</p></td><td></td><td></td><td align=\"right\"><p>kr 582</p></td><td align=\"right\"><p>kr 291</p></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td align=\"right\"><p> <strong>kr</strong> <strong>5 912</strong></p></td></tr></tbody></table><p>Pris for denne brudepakken er 6&#xa0;000 kroner (runda opp).</p><p>Nedanfor kan du laste ned eit rekneark med utrekninga over.</p><div data-type=\"file\"><ndlaembed data-path=\"/files/resources/omVoa1cUQ0fcNms.xlsx\" data-type=\"xlsx\" data-title=\"Priskalkyle brudepakke\" data-resource=\"file\"></ndlaembed></div></details><div data-type=\"related-content\"><ndlaembed data-resource=\"related-content\" data-article-id=\"32974\"></ndlaembed><ndlaembed data-resource=\"related-content\" data-article-id=\"33129\"></ndlaembed></div></section>", "language": "nn"}], "created": "2022-01-22T20:15:36Z", "started": false, "updated": "2022-11-25T08:22:21Z", "comments": [], "priority": "unspecified", "copyright": {"license": "CC-BY-SA-4.0", "creators": [{"name": "Hanne Merethe Riskedal Staurland", "type": "originator"}], "processed": false, "processors": [{"name": "Inger Gilje Sporaland", "type": "editorial"}, {"name": "Bjarne Skurdal", "type": "editorial"}], "rightsholders": []}, "grepCodes": ["KE219", "KM2503", "KM368", "KM369", "KM343", "KM344"], "metaImage": [{"altText": "Brudefrisyre. Foto.", "imageId": "51643", "language": "nb"}, {"altText": "Brudefrisyre. Foto.", "imageId": "51643", "language": "nn"}], "published": "2022-01-22T20:15:36Z", "updatedBy": "RrpsMfVOVjSoBnfSFDv2a-7K", "conceptIds": [], "articleType": "standard", "availability": "everyone", "editorLabels": [], "introduction": [{"language": "nb", "introduction": "Det er vanlig å tilby bruden en pakke bestående av prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlover. Dette er ofte en hyggelig stund for brud og forlover og inkluderer gjerne lette forfriskninger. Her er eksempel på utregning av pris."}, {"language": "nn", "introduction": "Det er vanleg å tilby bruda ein pakke med prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlovar. Dette er ofte ei hyggeleg stund for brud og forlovar og inkluderer gjerne lette forfriskingar. Her er døme på utrekning av pris."}], "revisionMeta": [{"id": "b31447f8-85ca-4e18-86ee-1745339442f3", "note": "Automatisk revisjonsdato satt av systemet.", "status": "needs-revision", "revisionDate": "2030-01-01T00:00:00Z"}], "visualElement": [], "relatedContent": [], "metaDescription": [{"content": "Utregning av pris på arbeider til brud i frisørsalong.", "language": "nb"}, {"content": "Utrekning av pris på arbeid til brud i frisørsalong.", "language": "nn"}], "requiredLibraries": [], "previousVersionsNotes": [{"note": "Opprettet artikkel.", "user": "CL4ahNTZlHEFfV0dYY8oWPuZ", "status": {"other": [], "current": "PLANNED"}, "timestamp": "2021-10-16T11:29:34Z"}, {"note": "Opprettet artikkel, som kopi av artikkel med id: '33237'.", "user": "CL4ahNTZlHEFfV0dYY8oWPuZ", "status": {"other": [], "current": "PLANNED"}, "timestamp": "2022-01-22T20:15:36Z"}, {"note": "Status endret", "user": "CL4ahNTZlHEFfV0dYY8oWPuZ", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-01-23T09:00:51Z"}, {"note": "Ny språkvariant 'en' ble lagt til.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-06T17:36:34Z"}, {"note": "Slettet språkvariant 'en'.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T12:07:37Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:00:42Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:13:52Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:14:09Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:14:23Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:14:38Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": ["IN_PROGRESS"], "current": "EXTERNAL_REVIEW"}, "timestamp": "2022-02-10T08:46:45Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-06-12T07:15:41Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-06-12T07:15:46Z"}, {"note": "Oppdatert taksonomi.", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-06-12T07:21:03Z"}, {"note": "Artikkelen har blitt lagret som ny versjon", "user": "455VO7UP9CLDSUU5TLugvgnc", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-10-21T13:42:20Z"}, {"note": "Ny språkvariant 'nn' ble lagt til.", "user": "455VO7UP9CLDSUU5TLugvgnc", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-10-28T11:57:40Z"}, {"note": "Status endret", "user": "455VO7UP9CLDSUU5TLugvgnc", "status": {"other": [], "current": "FOR_APPROVAL"}, "timestamp": "2022-10-28T12:06:11Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "END_CONTROL"}, "timestamp": "2022-11-18T11:56:46Z"}, {"note": "Status endret", "user": "a6N5hN1jq74SmIQBuwREHDW5", "status": {"other": [], "current": "PUBLISHED"}, "timestamp": "2022-11-18T12:35:55Z"}]}"""
      val expectedDocument =
        """{"tags": [{"tags": ["kalkulasjon", "økonomi", "brud", "bryllup", "frisør"], "language": "nb"}, {"tags": ["kalkulasjon", "økonomi", "brud", "bryllaup", "frisør"], "language": "nn"}], "notes": [{"note": "Oppdatert taksonomi.", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "PUBLISHED"}, "timestamp": "2022-11-25T08:22:21Z"}], "title": [{"title": "Utregning av pris på brudepakke hos frisør", "language": "nb"}, {"title": "Utrekning av pris på brudepakke hos frisør", "language": "nn"}], "status": {"other": [], "current": "PUBLISHED"}, "content": [{"content": "<section><p> </p><ndlaembed data-resource=\"image\" data-resource_id=\"51158\" data-size=\"full\" data-align=\"\" data-alt=\"Vakker brud som ser på blomsterbuketten sin. Foto.\" data-caption=\"Hva kan en brudepakke hos frisør og makeupartist koste?\"></ndlaembed><h2>Eksempel på utregning av pris på brudepakke</h2><p>En flott frisyre er viktig på den store dagen. En prøvetime gir frisøren mulighet til å bli kjent med kunden og håret hennes. Dere kan planlegge den frisyren som framhever kundens trekk og passer til brudekjolen.</p><ndlaembed data-resource=\"image\" data-resource_id=\"48590\" data-size=\"full\" data-align=\"\" data-alt=\"Krølling av hår på en kvinne hos en frisør. Både kunden og frisøren smiler. Foto.\" data-caption=\"God dialog og planlegging er viktig i en prøvetime.\"></ndlaembed><div data-type=\"framed-content\"><h3>Begreper</h3><p><strong>Påslagstallet </strong>er tallet du ganger innkjøpspris med for å finne ut hva kunden må betale for et produkt. Dette tallet ligger som regel mellom 2,5 og 6.</p><p><strong>Merverdiavgift (mva.)</strong> er en avgift til staten på salg av produkter. I en påslagskalkyle er merverdiavgiften inkludert.</p></div><h2>Oppgave</h2><p>Sett opp en priskalkyle etter oppsettet nedenfor, der alt i brudepakken er inkludert.</p><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td><p> </p></td></tr></tbody></table><details><summary>Løsning</summary><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Beskrivelse</p></th><th scope=\"col\"><p>Antall</p></th><th scope=\"col\"><p>Innkjøpspris per stk. uten mva.</p></th><th scope=\"col\"><p>Påslags-tall</p></th><th scope=\"col\"><p>Utsalgspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalgspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkter til hår</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 300</p></td><td data-align=\"right\"><p>kr 300</p></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 120</p></td><td data-align=\"right\"><p>kr 120</p></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffe og konfekt</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td data-align=\"right\"><p>kr 700</p></td><td data-align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td data-align=\"right\"><p>kr 700</p></td><td data-align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 600</p></td><td data-align=\"right\"><p>kr 600</p></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 873</p></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspenger</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspenger</p></th><td><p>2</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 1 164</p></td></tr><tr><th scope=\"row\"><p>Styling forlover</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Makeup forlover</p></th><td><p>0,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 291</p></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td data-align=\"right\"><p> <strong>kr</strong> <strong>5 912</strong></p></td></tr></tbody></table><p>Pris for denne brudepakken er 6&#xa0;000 kroner (rundet opp).</p><p>Nedenfor kan du laste ned et regneark med utregningen over.</p><div data-type=\"file\"><ndlaembed data-path=\"/files/resources/luM8b5Z3ueGdJK2.xlsx\" data-type=\"xlsx\" data-title=\"Priskalkyle brudepakke\" data-resource=\"file\"></ndlaembed></div></details><div data-type=\"related-content\"><ndlaembed data-resource=\"related-content\" data-article-id=\"32974\"></ndlaembed><ndlaembed data-resource=\"related-content\" data-article-id=\"33129\"></ndlaembed></div></section>", "language": "nb"}, {"content": "<section><p> </p><ndlaembed data-resource=\"image\" data-resource_id=\"51158\" data-size=\"full\" data-align=\"\" data-alt=\"Vakker brud som ser på blomsterbuketten sin. Foto.\" data-caption=\"Kva kan ein brudepakke hos frisør og makeupartist koste?\"></ndlaembed><h2>Døme på utrekning av pris på brudepakke</h2><p>Ein flott frisyre er viktig på den store dagen. Ein prøvetime gir frisøren moglegheit til å bli kjend med kunden og håret hennar. De kan planlegge den frisyren som framhevar trekka til kunden og passar til brudekjolen.</p><ndlaembed data-resource=\"image\" data-resource_id=\"48590\" data-size=\"full\" data-align=\"\" data-alt=\"Krølling av hår på ei kvinne hos ein frisør. Både kunden og frisøren smiler. Foto.\" data-caption=\"God dialog og planlegging er viktig i ein prøvetime.\"></ndlaembed><div data-type=\"framed-content\"><h3>Omgrep</h3><p><strong>Påslagstalet </strong>er talet du gonger innkjøpspris med for å finne ut kva kunden må betale for eit produkt. Dette talet ligg som regel mellom 2,5 og 6.</p><p><strong>Meirverdiavgift (mva.)</strong> er ei avgift til staten på sal av produkt. I ein påslagskalkyle er meirverdiavgifta inkludert.</p></div><h2>Oppgåve</h2><p>Set opp ein priskalkyle etter oppsettet nedanfor, der alt i brudepakken er inkludert.</p><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Skildring</p></th><th scope=\"col\"><p>Tal</p></th><th scope=\"col\"><p>Innkjøpspris per stk. utan mva.</p></th><th scope=\"col\"><p>Påslags-tal</p></th><th scope=\"col\"><p>Utsalspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkt til hår</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffi og konfekt</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td></td><td></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspengar</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspengar</p></th><td><p>2</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Styling forlovar</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"><p>Makeup forlovar</p></th><td><p>0,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td><p> </p></td></tr></tbody></table><details><summary>Løysing</summary><table><caption>priskalkyle</caption><thead><tr><th scope=\"col\"><p>Skildring</p></th><th scope=\"col\"><p>Tal</p></th><th scope=\"col\"><p>Innkjøpspris per stk. utan mva.</p></th><th scope=\"col\"><p>Påslags-tal</p></th><th scope=\"col\"><p>Utsalspris per stk. inkl. mva.</p></th><th scope=\"col\"><p>Total utsalspris inkl. mva.</p></th></tr></thead><tbody><tr><th scope=\"row\"><p>Produkt til hår</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 75</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 300</p></td><td data-align=\"right\"><p>kr 300</p></td></tr><tr><th scope=\"row\"><p>Spenner, hårnåler</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 30</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 120</p></td><td data-align=\"right\"><p>kr 120</p></td></tr><tr><th scope=\"row\"><p>Eplemost, frukt, kaffi og konfekt</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td data-align=\"right\"><p>kr 700</p></td><td data-align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Blomsterkrans, enkel</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 350</p></td><td><p>2</p></td><td data-align=\"right\"><p>kr 700</p></td><td data-align=\"right\"><p>kr 700</p></td></tr><tr><th scope=\"row\"><p>Makeup</p></th><td><p>1</p></td><td data-align=\"right\"><p>kr 150</p></td><td><p>4</p></td><td data-align=\"right\"><p>kr 600</p></td><td data-align=\"right\"><p>kr 600</p></td></tr><tr><th scope=\"row\"><p>Prøvetime/ konsultasjon</p></th><td><p>1,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 873</p></td></tr><tr><th scope=\"row\"><p>Makeupartist arbeidspengar</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Frisør arbeidspengar</p></th><td><p>2</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 1 164</p></td></tr><tr><th scope=\"row\"><p>Styling forlovar</p></th><td><p>1</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 582</p></td></tr><tr><th scope=\"row\"><p>Makeup forlovar</p></th><td><p>0,5</p></td><td></td><td></td><td data-align=\"right\"><p>kr 582</p></td><td data-align=\"right\"><p>kr 291</p></td></tr><tr><th scope=\"row\"></th><td></td><td></td><td></td><td></td><td></td></tr><tr><th scope=\"row\"><p><strong>Sluttsum</strong> <strong>brudepakke</strong></p></th><td></td><td></td><td></td><td></td><td data-align=\"right\"><p> <strong>kr</strong> <strong>5 912</strong></p></td></tr></tbody></table><p>Pris for denne brudepakken er 6&#xa0;000 kroner (runda opp).</p><p>Nedanfor kan du laste ned eit rekneark med utrekninga over.</p><div data-type=\"file\"><ndlaembed data-path=\"/files/resources/omVoa1cUQ0fcNms.xlsx\" data-type=\"xlsx\" data-title=\"Priskalkyle brudepakke\" data-resource=\"file\"></ndlaembed></div></details><div data-type=\"related-content\"><ndlaembed data-resource=\"related-content\" data-article-id=\"32974\"></ndlaembed><ndlaembed data-resource=\"related-content\" data-article-id=\"33129\"></ndlaembed></div></section>", "language": "nn"}], "created": "2022-01-22T20:15:36Z", "started": false, "updated": "2022-11-25T08:22:21Z", "comments": [], "priority": "unspecified", "copyright": {"license": "CC-BY-SA-4.0", "creators": [{"name": "Hanne Merethe Riskedal Staurland", "type": "originator"}], "processed": false, "processors": [{"name": "Inger Gilje Sporaland", "type": "editorial"}, {"name": "Bjarne Skurdal", "type": "editorial"}], "rightsholders": []}, "grepCodes": ["KE219", "KM2503", "KM368", "KM369", "KM343", "KM344"], "metaImage": [{"altText": "Brudefrisyre. Foto.", "imageId": "51643", "language": "nb"}, {"altText": "Brudefrisyre. Foto.", "imageId": "51643", "language": "nn"}], "published": "2022-01-22T20:15:36Z", "updatedBy": "RrpsMfVOVjSoBnfSFDv2a-7K", "conceptIds": [], "articleType": "standard", "availability": "everyone", "editorLabels": [], "introduction": [{"language": "nb", "introduction": "Det er vanlig å tilby bruden en pakke bestående av prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlover. Dette er ofte en hyggelig stund for brud og forlover og inkluderer gjerne lette forfriskninger. Her er eksempel på utregning av pris."}, {"language": "nn", "introduction": "Det er vanleg å tilby bruda ein pakke med prøvetime/konsultasjon, brudefrisering med makeup og oppsett eller styling av forlovar. Dette er ofte ei hyggeleg stund for brud og forlovar og inkluderer gjerne lette forfriskingar. Her er døme på utrekning av pris."}], "revisionMeta": [{"id": "b31447f8-85ca-4e18-86ee-1745339442f3", "note": "Automatisk revisjonsdato satt av systemet.", "status": "needs-revision", "revisionDate": "2030-01-01T00:00:00Z"}], "visualElement": [], "relatedContent": [], "metaDescription": [{"content": "Utregning av pris på arbeider til brud i frisørsalong.", "language": "nb"}, {"content": "Utrekning av pris på arbeid til brud i frisørsalong.", "language": "nn"}], "requiredLibraries": [], "previousVersionsNotes": [{"note": "Opprettet artikkel.", "user": "CL4ahNTZlHEFfV0dYY8oWPuZ", "status": {"other": [], "current": "PLANNED"}, "timestamp": "2021-10-16T11:29:34Z"}, {"note": "Opprettet artikkel, som kopi av artikkel med id: '33237'.", "user": "CL4ahNTZlHEFfV0dYY8oWPuZ", "status": {"other": [], "current": "PLANNED"}, "timestamp": "2022-01-22T20:15:36Z"}, {"note": "Status endret", "user": "CL4ahNTZlHEFfV0dYY8oWPuZ", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-01-23T09:00:51Z"}, {"note": "Ny språkvariant 'en' ble lagt til.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-06T17:36:34Z"}, {"note": "Slettet språkvariant 'en'.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T12:07:37Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:00:42Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:13:52Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:14:09Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:14:23Z"}, {"note": "Oppdatert taksonomi.", "user": "W7wNOzULDPE5SDuQE9aK-AJX", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-02-07T13:14:38Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": ["IN_PROGRESS"], "current": "EXTERNAL_REVIEW"}, "timestamp": "2022-02-10T08:46:45Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "IN_PROGRESS"}, "timestamp": "2022-06-12T07:15:41Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-06-12T07:15:46Z"}, {"note": "Oppdatert taksonomi.", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-06-12T07:21:03Z"}, {"note": "Artikkelen har blitt lagret som ny versjon", "user": "455VO7UP9CLDSUU5TLugvgnc", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-10-21T13:42:20Z"}, {"note": "Ny språkvariant 'nn' ble lagt til.", "user": "455VO7UP9CLDSUU5TLugvgnc", "status": {"other": [], "current": "LANGUAGE"}, "timestamp": "2022-10-28T11:57:40Z"}, {"note": "Status endret", "user": "455VO7UP9CLDSUU5TLugvgnc", "status": {"other": [], "current": "FOR_APPROVAL"}, "timestamp": "2022-10-28T12:06:11Z"}, {"note": "Status endret", "user": "RrpsMfVOVjSoBnfSFDv2a-7K", "status": {"other": [], "current": "END_CONTROL"}, "timestamp": "2022-11-18T11:56:46Z"}, {"note": "Status endret", "user": "a6N5hN1jq74SmIQBuwREHDW5", "status": {"other": [], "current": "PUBLISHED"}, "timestamp": "2022-11-18T12:35:55Z"}]}"""

      val migration = new V58__RenameAlignAttributeTable
      val result    = migration.convertDocument(oldDocument)
      result should be(expectedDocument)
    }
  }

}
