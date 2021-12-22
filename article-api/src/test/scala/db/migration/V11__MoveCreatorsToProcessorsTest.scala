/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V11__MoveCreatorsToProcessorsTest extends UnitSuite with TestEnvironment {
  val migration = new V11__MoveCreatorsToProcessors

  test("migration should move editorials from creators to processors") {
    val before =
      """{"copyright":{"license":"by-sa","origin":"","creators":[{"type":"Editorial","name":"Henrik"},{"type":"writer","name":"Henrik"}],"processors":[],"rightsholders"[]}}"""
    val expected =
      """{"copyright":{"license":"by-sa","origin":"","creators":[{"type":"writer","name":"Henrik"}],"processors":[{"type":"Editorial","name":"Henrik"}],"rightsholders":[]}}"""

    migration.convertArticleUpdate(before) should equal(expected)
  }

  test("migration not do anyhting if the document is already is converted") {
    val before =
      """{"copyright":{"license":"by-sa","origin":"","creators":[{"type":"writer","name":"Henrik"}],"processors":[{"type":"Editorial","name":"Henrik"}],"rightsholders":[]}}"""
    val expected = before

    migration.convertArticleUpdate(before) should equal(expected)
  }

}
