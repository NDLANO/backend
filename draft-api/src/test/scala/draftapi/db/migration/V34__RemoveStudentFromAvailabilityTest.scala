/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package draftapi.db.migration

import draftapi.db.migration.V34__RemoveStudentFromAvailability
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V34__RemoveStudentFromAvailabilityTest extends UnitSuite with TestEnvironment {
  val migration = new V34__RemoveStudentFromAvailability

  val learningResourceBeforeAvailabilityStudent =
    """{"id":3980,"tags":[{"tags":["wechat","lytteferdighet","muntlig ferdighet","sosialae medier","uttale"],"language":"unknown"}],"notes":[{"note":"muntlige øvelser","user":"System","status":{"other":["IMPORTED"],"current":"PUBLISHED"},"timestamp":"2017-06-22T01:59:20Z"}],"title":[{"title":"WeChat muntlige øvelser","language":"unknown"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><embed data-resource=\"h5p\" data-path=\"/resource/d427f0ed-9c00-4d5a-94f5-33e97d083ae0\"></section>","language":"unknown"}],"created":"2017-04-08T19:53:53Z","updated":"2017-06-22T01:59:20Z","revision":1,"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Ole Fossgård","type":"Writer"}],"processors":[{"name":"Luying Wang Belsnes","type":"Editorial"}],"rightsholders":[]},"grepCodes":[],"metaImage":[{"imageId":"5771","altText":"Kinesiske tegn. Betydning: muntlig. Illustrasjon.","language":"unknown"}],"published":"2017-06-22T01:59:20Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","conceptIds":[],"articleType":"standard","availability":"student","editorLabels":[],"introduction":[],"visualElement":[],"relatedContent":[],"metaDescription":[{"content":"Muntlige øvelser knyttet til leksjonen WeChat.","language":"unknown"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""

  val learningResourceBeforeAvailabilityEveryone =
    """{"id":3980,"tags":[{"tags":["wechat","lytteferdighet","muntlig ferdighet","sosialae medier","uttale"],"language":"unknown"}],"notes":[{"note":"muntlige øvelser","user":"System","status":{"other":["IMPORTED"],"current":"PUBLISHED"},"timestamp":"2017-06-22T01:59:20Z"}],"title":[{"title":"WeChat muntlige øvelser","language":"unknown"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><embed data-resource=\"h5p\" data-path=\"/resource/d427f0ed-9c00-4d5a-94f5-33e97d083ae0\"></section>","language":"unknown"}],"created":"2017-04-08T19:53:53Z","updated":"2017-06-22T01:59:20Z","revision":1,"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Ole Fossgård","type":"Writer"}],"processors":[{"name":"Luying Wang Belsnes","type":"Editorial"}],"rightsholders":[]},"grepCodes":[],"metaImage":[{"imageId":"5771","altText":"Kinesiske tegn. Betydning: muntlig. Illustrasjon.","language":"unknown"}],"published":"2017-06-22T01:59:20Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","conceptIds":[],"articleType":"standard","availability":"everyone","editorLabels":[],"introduction":[],"visualElement":[],"relatedContent":[],"metaDescription":[{"content":"Muntlige øvelser knyttet til leksjonen WeChat.","language":"unknown"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""

  val learningResourceBeforeAvailabilityTeacher =
    """{"id":3980,"tags":[{"tags":["wechat","lytteferdighet","muntlig ferdighet","sosialae medier","uttale"],"language":"unknown"}],"notes":[{"note":"muntlige øvelser","user":"System","status":{"other":["IMPORTED"],"current":"PUBLISHED"},"timestamp":"2017-06-22T01:59:20Z"}],"title":[{"title":"WeChat muntlige øvelser","language":"unknown"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><embed data-resource=\"h5p\" data-path=\"/resource/d427f0ed-9c00-4d5a-94f5-33e97d083ae0\"></section>","language":"unknown"}],"created":"2017-04-08T19:53:53Z","updated":"2017-06-22T01:59:20Z","revision":1,"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Ole Fossgård","type":"Writer"}],"processors":[{"name":"Luying Wang Belsnes","type":"Editorial"}],"rightsholders":[]},"grepCodes":[],"metaImage":[{"imageId":"5771","altText":"Kinesiske tegn. Betydning: muntlig. Illustrasjon.","language":"unknown"}],"published":"2017-06-22T01:59:20Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","conceptIds":[],"articleType":"standard","availability":"teacher","editorLabels":[],"introduction":[],"visualElement":[],"relatedContent":[],"metaDescription":[{"content":"Muntlige øvelser knyttet til leksjonen WeChat.","language":"unknown"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""

  val learningResourceAfterAvailabilityEveryone =
    """{"id":3980,"tags":[{"tags":["wechat","lytteferdighet","muntlig ferdighet","sosialae medier","uttale"],"language":"unknown"}],"notes":[{"note":"muntlige øvelser","user":"System","status":{"other":["IMPORTED"],"current":"PUBLISHED"},"timestamp":"2017-06-22T01:59:20Z"}],"title":[{"title":"WeChat muntlige øvelser","language":"unknown"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><embed data-resource=\"h5p\" data-path=\"/resource/d427f0ed-9c00-4d5a-94f5-33e97d083ae0\"></section>","language":"unknown"}],"created":"2017-04-08T19:53:53Z","updated":"2017-06-22T01:59:20Z","revision":1,"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Ole Fossgård","type":"Writer"}],"processors":[{"name":"Luying Wang Belsnes","type":"Editorial"}],"rightsholders":[]},"grepCodes":[],"metaImage":[{"imageId":"5771","altText":"Kinesiske tegn. Betydning: muntlig. Illustrasjon.","language":"unknown"}],"published":"2017-06-22T01:59:20Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","conceptIds":[],"articleType":"standard","availability":"everyone","editorLabels":[],"introduction":[],"visualElement":[],"relatedContent":[],"metaDescription":[{"content":"Muntlige øvelser knyttet til leksjonen WeChat.","language":"unknown"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""

  val learningResourceAfterAvailabilityTeacher =
    """{"id":3980,"tags":[{"tags":["wechat","lytteferdighet","muntlig ferdighet","sosialae medier","uttale"],"language":"unknown"}],"notes":[{"note":"muntlige øvelser","user":"System","status":{"other":["IMPORTED"],"current":"PUBLISHED"},"timestamp":"2017-06-22T01:59:20Z"}],"title":[{"title":"WeChat muntlige øvelser","language":"unknown"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><embed data-resource=\"h5p\" data-path=\"/resource/d427f0ed-9c00-4d5a-94f5-33e97d083ae0\"></section>","language":"unknown"}],"created":"2017-04-08T19:53:53Z","updated":"2017-06-22T01:59:20Z","revision":1,"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Ole Fossgård","type":"Writer"}],"processors":[{"name":"Luying Wang Belsnes","type":"Editorial"}],"rightsholders":[]},"grepCodes":[],"metaImage":[{"imageId":"5771","altText":"Kinesiske tegn. Betydning: muntlig. Illustrasjon.","language":"unknown"}],"published":"2017-06-22T01:59:20Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","conceptIds":[],"articleType":"standard","availability":"teacher","editorLabels":[],"introduction":[],"visualElement":[],"relatedContent":[],"metaDescription":[{"content":"Muntlige øvelser knyttet til leksjonen WeChat.","language":"unknown"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""

  migration.convertArticleUpdate(learningResourceBeforeAvailabilityStudent) should equal(
    learningResourceAfterAvailabilityEveryone
  )
  migration.convertArticleUpdate(learningResourceBeforeAvailabilityEveryone) should equal(
    learningResourceAfterAvailabilityEveryone
  )
  migration.convertArticleUpdate(learningResourceBeforeAvailabilityTeacher) should equal(
    learningResourceAfterAvailabilityTeacher
  )
}
