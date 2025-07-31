/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

import no.ndla.testbase.UnitTestSuiteBase

class V22__SplitTagsTest extends UnitTestSuiteBase {

  val migration = new V22__SplitTags

  test("That converting colon separated tags works") {
    val oldDocument =
      """{"tags":[{"tags":["tag1:tag2:tag3"],"language":"nb"},{"tags":["tag4:tag5:tag6"],"language":"en"}]}"""
    val converted        = migration.convertColumn(oldDocument)
    val expectedDocument =
      """{"tags":[{"tags":["tag1","tag2","tag3"],"language":"nb"},{"tags":["tag4","tag5","tag6"],"language":"en"}]}"""
    converted should be(expectedDocument)
  }

  test("That converting colon separated tags works with empty values") {
    val oldDocument      = """{"tags":[{"tags":["tag1::","apekatt",":snabeldyr:"],"language":"nb"}]}"""
    val converted        = migration.convertColumn(oldDocument)
    val expectedDocument = """{"tags":[{"tags":["tag1","apekatt","snabeldyr"],"language":"nb"}]}"""
    converted should be(expectedDocument)
  }

  test("That non colon separated tags are not changed") {
    val oldDocument      = """{"tags":[{"tags":["tag1","apekatt","snabeldyr"],"language":"nb"}]}"""
    val converted        = migration.convertColumn(oldDocument)
    val expectedDocument = """{"tags":[{"tags":["tag1","apekatt","snabeldyr"],"language":"nb"}]}"""
    converted should be(expectedDocument)
  }

  test("That no tags doesn't crash") {
    val oldDocument = """{"someotherfield":"yabadabado"}"""
    val converted   = migration.convertColumn(oldDocument)
    converted should be(oldDocument)
  }
}
