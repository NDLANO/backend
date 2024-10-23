/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.common.model.domain.{ArticleContent, Title}
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V65__RemoveStrongFromTitleTest extends UnitSuite with TestEnvironment {
  test("That strong are removed from title") {
    val oldTitle      = Title("This is a <strong>title</strong>", language = "nb")
    val expectedTitle = Title("This is a title", language = "nb")

    val migration = new V65__RemoveStrongFromTitle
    val result    = migration.convertTitle(oldTitle)
    result should be(expectedTitle)
  }

  test("That nested strong are removed from title") {
    val oldTitle      = Title("This is a <strong><em>title</em></strong>", language = "nb")
    val expectedTitle = Title("This is a <em>title</em>", language = "nb")

    val migration = new V65__RemoveStrongFromTitle
    val result    = migration.convertTitle(oldTitle)
    result should be(expectedTitle)
  }

  test("That strong are removed from title in article") {
    val oldContent =
      ArticleContent("<section><h2>This is a <strong>title</strong></h2><p>Some text</p></section>", language = "nb")
    val expectedContent = ArticleContent("<section><h2>This is a title</h2><p>Some text</p></section>", language = "nb")

    val migration = new V65__RemoveStrongFromTitle
    val result    = migration.convertContent(oldContent)
    result should be(expectedContent)
  }
}
