/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V54__BodyBoxToFramedContextTestTest extends UnitSuite with TestEnvironment {

  test("That body boxes are converted to framed context") {
    val oldDocument =
      """{"content":[{"content":"<section><div class=\"c-bodybox\"><h3>Jadda</h3></div></section>","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"content":[{"content":"<section><div data-type=\"framed-content\"><h3>Jadda</h3></div></section>","language":"nb"}],"articleType":"standard"}"""

    val migration = new V54__BodyBoxToFramedContext
    val result    = migration.convertDocument(oldDocument)
    result should be(expectedDocument)
  }

  test("That body boxes are converted to framed context even with other classes") {
    val oldDocument =
      """{"content":[{"content":"<section><div class=\"c-bodybox apekatt\"><h3>Jadda</h3></div></section>","language":"nb"}],"articleType":"standard"}"""
    val expectedDocument =
      """{"content":[{"content":"<section><div class=\"apekatt\" data-type=\"framed-content\"><h3>Jadda</h3></div></section>","language":"nb"}],"articleType":"standard"}"""

    val migration = new V54__BodyBoxToFramedContext
    val result    = migration.convertDocument(oldDocument)
    result should be(expectedDocument)
  }
}
