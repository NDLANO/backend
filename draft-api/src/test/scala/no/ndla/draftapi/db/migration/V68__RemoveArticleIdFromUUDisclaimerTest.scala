/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V68__RemoveArticleIdFromUUDisclaimerTest extends UnitSuite with TestEnvironment {
  test("That article-id is removed from uu-disclaimer") {
    val migration  = new V68__RemoveArticleIdFromUUDisclaimer
    val oldArticle =
      """<section><ndlaembed data-resource="uu-disclaimer" data-disclaimer="Dette innholdet er ikke universelt utformet, og noen brukere kan derfor ha problemer med å oppfatte og forstå det." data-article-id="38293"><p>Hallo!</p></ndlaembed></section>"""
    val newArticle =
      """<section><ndlaembed data-resource="uu-disclaimer" data-disclaimer="Dette innholdet er ikke universelt utformet, og noen brukere kan derfor ha problemer med å oppfatte og forstå det."><p>Hallo!</p></ndlaembed></section>"""

    migration.convertContent(oldArticle, "nb") should be(newArticle)
  }
}
