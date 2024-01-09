/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V53__AddSolvedToCommentsTest extends UnitSuite with TestEnvironment {

  test("That solved is added to comments") {
    val oldDocument =
      """{"comments":[{"content":"hei","id":"123"},{"content":"hei","id":"1234"}],"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"comments":[{"content":"hei","id":"123","solved":false},{"content":"hei","id":"1234","solved":false}],"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val migration = new V53__AddSolvedToComments
    val result    = migration.convertDocument(oldDocument)
    result should be(expectedDocument)
  }

  test("that we dont break document with no comments") {
    val oldDocument =
      """{"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"title":[{"title":"hei","language":"nb"}],"articleType":"standard"}"""
    val migration = new V53__AddSolvedToComments
    val result    = migration.convertDocument(oldDocument)
    result should be(expectedDocument)
  }
}
