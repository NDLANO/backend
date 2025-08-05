/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V73__InlineConceptLinkTextToChildrenTest extends UnitSuite with TestEnvironment {
  test("That data-link-text attributes are moved to the children of the ndlaembed") {
    val migration = new V73__InlineConceptLinkTextToChildren
    val oldEmbed  =
      """<ndlaembed data-content-id="4242" data-link-text="Foobar" data-resource="concept" data-type="inline"></ndlaembed>"""
    val newEmbed =
      """<ndlaembed data-content-id="4242" data-resource="concept" data-type="inline">Foobar</ndlaembed>"""

    migration.convertContent(oldEmbed, "nb") should be(newEmbed)
  }
}
