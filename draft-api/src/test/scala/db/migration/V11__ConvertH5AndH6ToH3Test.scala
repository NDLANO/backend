/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V11__ConvertH5AndH6ToH3Test extends UnitSuite with TestEnvironment {

  val migration = new V11__ConvertH5AndH6ToH3

  test("migration should convert h5 and h6 html tags to h3") {
    val old =
      s"""{"content":[{"content":"<section><h5>hi</h5></section>","language":"nb"},{"content":"<section><h6>hi</h6></section>","language":"nn"}],"title":[{"title":"tittel","language":"nb"}]}"""
    val expected =
      s"""{"content":[{"content":"<section><p><strong>hi</strong></p></section>","language":"nb"},{"content":"<section>hi</section>","language":"nn"}],"title":[{"title":"tittel","language":"nb"}]}"""
    migration.convertArticleUpdate(old) should equal(expected)
  }

  test("migration should not convert any other tags") {
    val original =
      s"""{"content":[{"content":"<section><h3>hi</h3></section>","language":"nb"},{"content":"<section><h3>hi</h3><p>heh</p></section>","language":"nn"}],"title":[{"title":"tittel","language":"nb"}]}"""
    migration.convertArticleUpdate(original) should equal(original)
  }

}
