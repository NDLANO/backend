/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V4__MoveRelatedContentEmbedsToDivsTest extends UnitSuite with TestEnvironment {
  val migration = new V4__MoveRelatedContentEmbedsToDivs

  test("migration should move embeds to divs") {
    val before =
      """{"content":[{"content":"<section><h1>Hello</h1></section><section><embed data-article-ids=\"20,22,25\" data-resource=\"related-content\"></section>", "language": "nb" },{"content":"<section><h1>Hello</h1></section><section><embed data-article-ids=\"20,22,25\" data-resource=\"related-content\"></section>","language":"nn"}]}"""
    val expected =
      """{"content":[{"content":"<section><h1>Hello</h1></section><section><div data-type=\"related-content\"><embed data-article-id=\"20\" data-resource=\"related-content\"><embed data-article-id=\"22\" data-resource=\"related-content\"><embed data-article-id=\"25\" data-resource=\"related-content\"></div></section>","language":"nb"},{"content":"<section><h1>Hello</h1></section><section><div data-type=\"related-content\"><embed data-article-id=\"20\" data-resource=\"related-content\"><embed data-article-id=\"22\" data-resource=\"related-content\"><embed data-article-id=\"25\" data-resource=\"related-content\"></div></section>","language":"nn"}]}"""

    migration.convertArticleUpdate(before) should equal(expected)
  }

  test("migration not do anyhting if the document is already is converted") {
    val before =
      """{"content":[{"content":"<section><h1>Hello</h1></section><section><div data-type=\"related-content\"><embed data-article-id=\"20\" data-resource=\"related-content\"><embed data-article-id=\"22\" data-resource=\"related-content\"><embed data-article-id=\"25\" data-resource=\"related-content\"></div></section>","language":"nb"},{"content":"<section><h1>Hello</h1></section><section><div data-type=\"related-content\"><embed data-article-id=\"20\" data-resource=\"related-content\"><embed data-article-id=\"22\" data-resource=\"related-content\"><embed data-article-id=\"25\" data-resource=\"related-content\"></div></section>","language":"nn"}]}"""
    val expected = before

    migration.convertArticleUpdate(before) should equal(expected)
  }

}
