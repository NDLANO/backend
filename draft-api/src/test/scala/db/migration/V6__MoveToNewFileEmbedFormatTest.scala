/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V6__MoveToNewFileEmbedFormatTest extends UnitSuite with TestEnvironment {
  val migration = new V6__MoveToNewFileEmbedFormat

  test("migration should remove url and add path to file-embeds") {
    val beforeFileDiv =
      """<div data-type=\"file\"><embed data-resource=\"file\" data-url=\"https://test.api.ndla.no/files/kek/fullavtrix.pdf\" data-name=\"full av trix du\"></div>"""
    val afterFileDiv =
      """<div data-type=\"file\"><embed data-resource=\"file\" data-name=\"full av trix du\" data-path=\"/files/kek/fullavtrix.pdf\"></div>"""

    val before =
      s"""{"content":[{"content":"<section><h1>Hello</h1>$beforeFileDiv</section>","language":"nb"},{"content":"<section><h1>Hello</h1>$beforeFileDiv</section>","language":"nn"}]}"""
    val expected =
      s"""{"content":[{"content":"<section><h1>Hello</h1>$afterFileDiv</section>","language":"nb"},{"content":"<section><h1>Hello</h1>$afterFileDiv</section>","language":"nn"}]}"""

    val result = migration.convertArticleUpdate(before)
    result should equal(expected)
  }

  test("migration should not affect new format file embeds") {
    val beforeFileDiv =
      """<div data-type=\"file\"><embed data-resource=\"file\" data-name=\"full av trix du\" data-path=\"/files/kek/fullavtrix.pdf\"></div>"""
    val afterFileDiv =
      """<div data-type=\"file\"><embed data-resource=\"file\" data-name=\"full av trix du\" data-path=\"/files/kek/fullavtrix.pdf\"></div>"""

    val before =
      s"""{"content":[{"content":"<section><h1>Hello</h1>$beforeFileDiv</section>","language":"nb"},{"content":"<section><h1>Hello</h1>$beforeFileDiv</section>","language":"nn"}]}"""
    val expected =
      s"""{"content":[{"content":"<section><h1>Hello</h1>$afterFileDiv</section>","language":"nb"},{"content":"<section><h1>Hello</h1>$afterFileDiv</section>","language":"nn"}]}"""

    val result = migration.convertArticleUpdate(before)
    result should equal(expected)
  }

}
