/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package db.migration
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class R__SetArticleTypeFromTaxonomyTest extends UnitSuite with TestEnvironment {
  val migration = new R__SetArticleTypeFromTaxonomy

  test("Articles that are 'topic's in taxonomy will be migrated to 'topic-article's") {
    val standard = """{"id":1,"articleType":"standard"}"""
    val topic = """{"id":1,"articleType":"topic-article"}"""

    migration.convertArticleUpdate(standard, 1, List(1), List()) should be(topic)
    migration.convertArticleUpdate(standard, 1, List(1), List(1)) should be(topic)
    migration.convertArticleUpdate(topic, 1, List(), List(1)) should be(standard)
    migration.convertArticleUpdate(topic, 1, List(), List()) should be(topic)
    migration.convertArticleUpdate(standard, 1, List(), List()) should be(standard)
  }
}
