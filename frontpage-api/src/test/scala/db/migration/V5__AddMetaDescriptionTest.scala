/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}

class V5__AddMetaDescriptionTest extends UnitSuite with TestEnvironment {
  val migration = new V5__add_meta_description

  test("that empty metaDescription is added") {
    val before =
      """{"name":"Kinesisk","layout":"double","bannerImage":{"mobileImageId":66,"desktopImageId":65},"about":[{"title":"Om kinesisk","description":"Kinesiskfaget gir en grunnleggende innsikt i levemåter og tankesett i Kina.","language":"nb","visualElement":{"type":"brightcove","id":"182071"}}],"mostRead":["urn:resource:1:148063"],"editorsChoices":["urn:resource:1:163488"],"goTo":[]}"""

    val after =
      """{"name":"Kinesisk","layout":"double","bannerImage":{"mobileImageId":66,"desktopImageId":65},"about":[{"title":"Om kinesisk","description":"Kinesiskfaget gir en grunnleggende innsikt i levemåter og tankesett i Kina.","language":"nb","visualElement":{"type":"brightcove","id":"182071"}}],"metaDescription":[],"mostRead":["urn:resource:1:148063"],"editorsChoices":["urn:resource:1:163488"],"goTo":[]}"""

    migration.convertSubjectpage(DBSubjectPage(1, before)).get.document should equal(after)
  }

}
