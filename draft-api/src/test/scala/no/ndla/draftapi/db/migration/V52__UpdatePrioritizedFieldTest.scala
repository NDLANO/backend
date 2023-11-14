/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V52__UpdatePrioritizedFieldTest extends UnitSuite with TestEnvironment {

  test("That prioritized is updated to priority and that value is mapped to correct priority type") {
    val oldDocument =
      """{"prioritized":true,"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"title":[{"title":"hei","language":"nb"}],"articleType":"standard","priority":"prioritized"}"""
    val migration = new V52__UpdatePrioritizedField
    val result    = migration.convertDocument(oldDocument)
    result should be(expectedDocument)
  }
}
