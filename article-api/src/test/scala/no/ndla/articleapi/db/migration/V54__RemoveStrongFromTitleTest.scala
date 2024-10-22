/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.Title

class V54__RemoveStrongFromTitleTest extends UnitSuite with TestEnvironment {
  test("That strong are removed from title") {
    val oldTitle      = Title("This is a <strong>title</strong>", language = "nb")
    val expectedTitle = Title("This is a title", language = "nb")

    val migration = new V54__RemoveStrongFromTitle
    val result    = migration.convertTitle(oldTitle)
    result should be(expectedTitle)
  }

  test("That nested strong are removed from title") {
    val oldTitle      = Title("This is a <strong><em>title</em></strong>", language = "nb")
    val expectedTitle = Title("This is a <em>title</em>", language = "nb")

    val migration = new V54__RemoveStrongFromTitle
    val result    = migration.convertTitle(oldTitle)
    result should be(expectedTitle)
  }
}
