/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V3__AddUpdatedColoumsTest extends UnitSuite with TestEnvironment {

  val migration = new V3__AddUpdatedColoums

  test("add updatedBy and updated on audio object") {
    val before =
      """{"tags": [{"tags": ["tastatur", "verktøyferdigheit"], "language": "nn"}, {"tags": ["skriveverktøy", "tastatur", "tekstproduksjon", "verktøyferdighet"], "language": "nb"}, {"tags": ["writing tools", "keyboard", "text production"]}, {"tags": ["writing tools", "keyboard", "text production"], "language": "en"}], "title": [{"title": "Hvor godt taster du?", "language": "nb"}, {"title": "Kor godt tastar du?", "language": "nn"}], "content": [{"content": "<section>\n        <p><embed data-resource=\"external\" data-url=\"http://typing-speed-test.aoeu.eu/\" /></p><p>Ta testen ovenfor eller bruk testen på  <a href=\"http://10fastfingers.com/typing-test/norwegian\" title=\"10fastfingers.com\"> 10fastfingers.com</a> om du heller vil ha norsk tekst. <strong></strong></p><p><embed data-resource=\"h5p\" data-url=\"//ndla.no/h5p/embed/159531\" /><strong><br /></strong></p></section>", "language": "nb"}, {"content": "<section>\n        <p><embed data-resource=\"external\" data-url=\"http://typing-speed-test.aoeu.eu/\" /></p><p>Ta testen ovanfor eller bruk testen på  <a href=\"http://10fastfingers.com/typing-test/norwegian\" title=\"10fastfingers.com\"> 10fastfingers.com</a> om du heller vil ha norsk tekst. <strong></strong></p><p><embed data-resource=\"h5p\" data-url=\"//ndla.no/h5p/embed/163537\" /><strong><br /></strong></p></section>", "language": "nn"}], "created": "2016-03-04T15:08:51Z", "updated": "2017-03-07T17:45:41Z", "copyright": {"origin": "", "authors": [{"name": "Ragnhild Tønnessen", "type": "Forfatter"}], "license": "by-sa"}, "contentType": "Oppgave", "introduction": [{"language": "nb", "introduction": "Hvor raskt taster du, og hvor mange feil gjør du? Test skrivehastigheten din her og noter resultatene i dokumentasjonsverktøyet nedenfor."}, {"language": "nn", "introduction": "Kor raskt tastar du, og kor mange feil gjer du? Test skrivefarten din her og noter resultata i dokumentasjonsverktøyet nedanfor."}], "visualElement": [], "metaDescription": [{"content": "Hvor raskt taster du, og hvor mange feil gjør du? Test skrivehastigheten din her og noter resultatene i dokumentasjonsverktøyet nedenfor.", "language": "nb"}, {"content": "Kor raskt tastar du, og kor mange feil gjer du? Test skrivefarten din her og noter resultata i dokumentasjonsverktøyet nedanfor.", "language": "nn"}], "requiredLibraries": [{"url": "//ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js", "name": "H5P-Resizer", "mediaType": "text/javascript"}]}"""
    val expectedAfter =
      """{"tags":[{"tags":["tastatur","verktøyferdigheit"],"language":"nn"},{"tags":["skriveverktøy","tastatur","tekstproduksjon","verktøyferdighet"],"language":"nb"},{"tags":["writing tools","keyboard","text production"]},{"tags":["writing tools","keyboard","text production"],"language":"en"}],"title":[{"title":"Hvor godt taster du?","language":"nb"},{"title":"Kor godt tastar du?","language":"nn"}],"content":[{"content":"<section>\n        <p><embed data-resource=\"external\" data-url=\"http://typing-speed-test.aoeu.eu/\" /></p><p>Ta testen ovenfor eller bruk testen på  <a href=\"http://10fastfingers.com/typing-test/norwegian\" title=\"10fastfingers.com\"> 10fastfingers.com</a> om du heller vil ha norsk tekst. <strong></strong></p><p><embed data-resource=\"h5p\" data-url=\"//ndla.no/h5p/embed/159531\" /><strong><br /></strong></p></section>","language":"nb"},{"content":"<section>\n        <p><embed data-resource=\"external\" data-url=\"http://typing-speed-test.aoeu.eu/\" /></p><p>Ta testen ovanfor eller bruk testen på  <a href=\"http://10fastfingers.com/typing-test/norwegian\" title=\"10fastfingers.com\"> 10fastfingers.com</a> om du heller vil ha norsk tekst. <strong></strong></p><p><embed data-resource=\"h5p\" data-url=\"//ndla.no/h5p/embed/163537\" /><strong><br /></strong></p></section>","language":"nn"}],"created":"2016-03-04T15:08:51Z","updated":"2017-03-07T17:45:41Z","copyright":{"origin":"","authors":[{"name":"Ragnhild Tønnessen","type":"Forfatter"}],"license":"by-sa"},"contentType":"Oppgave","introduction":[{"language":"nb","introduction":"Hvor raskt taster du, og hvor mange feil gjør du? Test skrivehastigheten din her og noter resultatene i dokumentasjonsverktøyet nedenfor."},{"language":"nn","introduction":"Kor raskt tastar du, og kor mange feil gjer du? Test skrivefarten din her og noter resultata i dokumentasjonsverktøyet nedanfor."}],"visualElement":[],"metaDescription":[{"content":"Hvor raskt taster du, og hvor mange feil gjør du? Test skrivehastigheten din her og noter resultatene i dokumentasjonsverktøyet nedenfor.","language":"nb"},{"content":"Kor raskt tastar du, og kor mange feil gjer du? Test skrivefarten din her og noter resultata i dokumentasjonsverktøyet nedanfor.","language":"nn"}],"requiredLibraries":[{"url":"//ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js","name":"H5P-Resizer","mediaType":"text/javascript"}],"updatedBy":"content-import-client"}"""

    val article = V3_DBArticleMetaInformation(1, before)
    val converted = migration.convertArticleUpdate(article)
    converted.document should equal(expectedAfter)

  }

}
