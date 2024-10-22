/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.common.model.domain.Title
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
}
