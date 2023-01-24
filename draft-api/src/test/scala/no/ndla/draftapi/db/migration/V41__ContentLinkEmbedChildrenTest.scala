/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V41__ContentLinkEmbedChildrenTest extends UnitSuite with TestEnvironment {

  test("That converting content-links works as expected") {
    val testHtml =
      """<section><ndlaembed data-content-id="13690" data-link-text="Fastset blodtypen ved hjelp av Blodtypar og blodoverføring" data-open-in="new-context" data-resource="content-link" data-content-type="article"></ndlaembed>.</section>"""
    val expectedResult =
      """<section><ndlaembed data-content-id="13690" data-open-in="new-context" data-resource="content-link" data-content-type="article">Fastset blodtypen ved hjelp av Blodtypar og blodoverføring</ndlaembed>.</section>"""
    val migration = new V41__ContentLinkEmbedChildren
    val result    = migration.moveContentLinkTextToChildren(testHtml)
    result should be(expectedResult)
  }
}
