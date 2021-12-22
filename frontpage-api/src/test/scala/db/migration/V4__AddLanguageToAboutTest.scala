/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration
import io.circe.syntax._
import no.ndla.frontpageapi.repository._
import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}

class V4__AddLanguageToAboutTest extends UnitSuite with TestEnvironment {
  val migration = new V4__add_language_to_about

  test("that aboutSubject is transformed") {
    val before =
      """{"name":"Kinesisk","layout":"double","bannerImage":{"mobileImageId":66,"desktopImageId":65},"about":{"title":"Om kinesisk","description":"Kinesiskfaget gir en grunnleggende innsikt i levemåter og tankesett i Kina.","visualElement":{"type":"brightcove","id":"182071"}},"mostRead":["urn:resource:1:148063"],"editorsChoices":["urn:resource:1:163488"],"goTo":[]}"""

    val after =
      """{"name":"Kinesisk","layout":"double","bannerImage":{"mobileImageId":66,"desktopImageId":65},"about":[{"title":"Om kinesisk","description":"Kinesiskfaget gir en grunnleggende innsikt i levemåter og tankesett i Kina.","language":"nb","visualElement":{"type":"brightcove","id":"182071"}}],"mostRead":["urn:resource:1:148063"],"editorsChoices":["urn:resource:1:163488"],"goTo":[]}"""

    migration.convertSubjectpage(DBSubjectPage(1, before)).get.document should equal(after)
  }

}
