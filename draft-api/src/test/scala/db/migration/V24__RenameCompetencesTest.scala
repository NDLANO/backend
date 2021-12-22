/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V24__RenameCompetencesTest extends UnitSuite with TestEnvironment {
  val migration = new V24__RenameCompetences

  test("migration should remove the competences field and add grepCodes instead") {
    val before = """{"competences":["1","2","3","asd"],"title":[{"title":"tittel","language":"nb"}]}"""
    val expected = """{"grepCodes":["1","2","3","asd"],"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(before) should equal(expected)
  }

  test("migration does nothing if the document is already converted") {
    val original = """{"grepCodes":["1","2","3","asd"],"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(original) should equal(original)
  }

}
