/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V20__DeleteUnintentionalNullValuesTest extends UnitSuite with TestEnvironment {
  val migration = new V20__DeleteUnintentionalNullValues

  test("That markdown is migrated correctly") {
    val original =
      """{"tags":[],"title":[{"title":"(spørsmålspartikkel); hva med?","language":"nb"},{"title":"(spørsmåls-partikkel); kva med...?","language":"nn"}],"status":{"other":[],"current":"PUBLISHED"},"content":[{"content":" ","language":"nb"},{"content":" ","language":"nn"}],"created":"2023-11-03T12:30:42.725Z","updated":"2023-11-03T12:30:42.815Z","glossData":{"gloss":"呢","examples":[[{"example":"我叫马红，你呢？//我叫馬紅，你呢？","language":"zh","transcriptions":{"pinyin":"Wǒ jiào Mǎ Hóng, nǐ ne?","traditional":null}},{"example":"Jeg heter Ma Hong, hva med deg?","language":"nb","transcriptions":{}},{"example":"Eg heiter Ma Hong, kva med deg?","language":"nn","transcriptions":{}}],[{"example":"我姓王，你呢？","language":"zh","transcriptions":{"pinyin":"Wǒ xìng Wáng, nǐ ne?","traditional":null}},{"example":"Jeg heter Wang til etternavn, hva med deg?","language":"nb","transcriptions":{}},{"example":"Eg heiter Wang til etternamn, kva med deg?","language":"nn","transcriptions":{}}],[{"example":"我是老师，你呢？//我是老師，你呢？","language":"zh","transcriptions":{"pinyin":"Wǒ shì lǎoshī, nǐ ne?","traditional":null}},{"example":"Jeg er lærer, hva med deg?","language":"nb","transcriptions":{}},{"example":"Eg er lærar, kva med deg?","language":"nn","transcriptions":{}}]],"wordClass":"particle","transcriptions":{"pinyin":"ne","traditional":null},"originalLanguage":"zh"},"metaImage":[],"updatedBy":["2GsATsYbFJww7gKHgrR74lye11vK9Kjy"],"articleIds":[],"subjectIds":[],"conceptType":"gloss","editorNotes":[],"visualElement":[{"language":"nb","visualElement":"<ndlaembed data-resource=\"audio\" data-resource_id=\"76\" data-type=\"standard\"></ndlaembed>"}],"supportedLanguages":null}"""
    val expected =
      """{"tags":[],"title":[{"title":"(spørsmålspartikkel); hva med?","language":"nb"},{"title":"(spørsmåls-partikkel); kva med...?","language":"nn"}],"status":{"other":[],"current":"PUBLISHED"},"content":[{"content":" ","language":"nb"},{"content":" ","language":"nn"}],"created":"2023-11-03T12:30:42.725Z","updated":"2023-11-03T12:30:42.815Z","glossData":{"gloss":"呢","examples":[[{"example":"我叫马红，你呢？//我叫馬紅，你呢？","language":"zh","transcriptions":{"pinyin":"Wǒ jiào Mǎ Hóng, nǐ ne?","traditional":""}},{"example":"Jeg heter Ma Hong, hva med deg?","language":"nb","transcriptions":{}},{"example":"Eg heiter Ma Hong, kva med deg?","language":"nn","transcriptions":{}}],[{"example":"我姓王，你呢？","language":"zh","transcriptions":{"pinyin":"Wǒ xìng Wáng, nǐ ne?","traditional":""}},{"example":"Jeg heter Wang til etternavn, hva med deg?","language":"nb","transcriptions":{}},{"example":"Eg heiter Wang til etternamn, kva med deg?","language":"nn","transcriptions":{}}],[{"example":"我是老师，你呢？//我是老師，你呢？","language":"zh","transcriptions":{"pinyin":"Wǒ shì lǎoshī, nǐ ne?","traditional":""}},{"example":"Jeg er lærer, hva med deg?","language":"nb","transcriptions":{}},{"example":"Eg er lærar, kva med deg?","language":"nn","transcriptions":{}}]],"wordClass":"particle","transcriptions":{"pinyin":"ne","traditional":""},"originalLanguage":"zh"},"metaImage":[],"updatedBy":["2GsATsYbFJww7gKHgrR74lye11vK9Kjy"],"articleIds":[],"subjectIds":[],"conceptType":"gloss","editorNotes":[],"visualElement":[{"language":"nb","visualElement":"<ndlaembed data-resource=\"audio\" data-resource_id=\"76\" data-type=\"standard\"></ndlaembed>"}],"supportedLanguages":null}"""

    val result = migration.convertToNewConcept(original)
    result should be(expected)
  }

}
