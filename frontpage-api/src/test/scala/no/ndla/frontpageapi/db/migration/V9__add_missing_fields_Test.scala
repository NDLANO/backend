/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.db.migration

import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}

class V9__add_missing_fields_Test extends UnitSuite with TestEnvironment {
  val migration = new V9__add_missing_fields

  test("that empty empty lists for connectedTo, buildsOn and leadsTo is added, and unused attributes are removed") {
    val before =
      """{"name":"Kinesisk","layout":"double","bannerImage":{"desktopImageId":65},"about":[{"title":"Om kinesisk","description":"Kinesiskfaget gir en grunnleggende innsikt i levemåter og tankesett i Kina.","language":"nb","visualElement":{"type":"brightcove","id":"182071"}}],"metaDescription":[],"mostRead":["urn:resource:1:148063"],"editorsChoices":["urn:resource:1:163488"],"goTo":[]}"""

    val after =
      """{"name":"Kinesisk","bannerImage":{"desktopImageId":65},"about":[{"title":"Om kinesisk","description":"Kinesiskfaget gir en grunnleggende innsikt i levemåter og tankesett i Kina.","language":"nb","visualElement":{"type":"brightcove","id":"182071"}}],"metaDescription":[],"editorsChoices":["urn:resource:1:163488"],"connectedTo":[],"buildsOn":[],"leadsTo":[]}"""

    migration.convertSubjectpage(DBSubjectPage(1, before)).document should be(after)
  }
}
