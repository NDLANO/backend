/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V4__AddArticleTypeField extends UnitSuite with TestEnvironment {
  val migration = new V4__AddUpdatedColoums

  test("migration should remove the contentType field and add an articleType field with value topic-article") {
    val withEmneartikkelBefore = """{"contentType":"emneartikkel"}"""
    val withEmneartikkelExpected = """{"articleType":"topic-article"}"""

    val article = V4_DBArticleMetaInformation(1, withEmneartikkelBefore)
    val converted = migration.convertArticleUpdate(article)
    converted.document should equal(withEmneartikkelExpected)
  }

  test("migration should remove the contentType field and add an articleType field with value standard") {
    val withEmneartikkelBefore = """{"contentType":"Oppgave"}"""
    val withEmneartikkelExpected = """{"articleType":"standard"}"""

    val article = V4_DBArticleMetaInformation(1, withEmneartikkelBefore)
    val converted = migration.convertArticleUpdate(article)
    converted.document should equal(withEmneartikkelExpected)
  }

  test("migration not do anyhting if the document is already is converted") {
    val withEmneartikkelBefore = """{"articleType":"standard"}"""
    val withEmneartikkelExpected = """{"articleType":"standard"}"""

    val article = V4_DBArticleMetaInformation(1, withEmneartikkelBefore)
    val converted = migration.convertArticleUpdate(article)
    converted.document should equal(withEmneartikkelExpected)
  }

}
