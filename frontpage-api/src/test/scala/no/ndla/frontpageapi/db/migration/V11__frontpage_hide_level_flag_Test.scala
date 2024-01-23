/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.db.migration

import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}

class V11__frontpage_hide_level_flag_Test extends UnitSuite with TestEnvironment {
  val migration = new V11__frontpage_hide_level_flag

  test("that visual element id-urls are converted to ids") {
    val before =
      """{"menu":[{"menu":[{"menu":[],"articleId":38259},{"menu":[],"articleId":38253},{"menu":[{"menu":[{"menu":[],"articleId":38207}],"articleId":38041}],"articleId":38260},{"menu":[],"articleId":38002}],"articleId":38247},{"menu":[],"articleId":38261}],"articleId":38140}"""
    val after =
      """{"menu":[{"menu":[{"menu":[],"articleId":38259,"hideLevelFlag":false},{"menu":[],"articleId":38253,"hideLevelFlag":false},{"menu":[{"menu":[{"menu":[],"articleId":38207,"hideLevelFlag":false}],"articleId":38041,"hideLevelFlag":false}],"articleId":38260,"hideLevelFlag":false},{"menu":[],"articleId":38002,"hideLevelFlag":false}],"articleId":38247,"hideLevelFlag":false},{"menu":[],"articleId":38261,"hideLevelFlag":false}],"articleId":38140}"""

    migration.convertFrontpage(V11__DBFrontPage(1, before)).document should be(after)
  }
}
