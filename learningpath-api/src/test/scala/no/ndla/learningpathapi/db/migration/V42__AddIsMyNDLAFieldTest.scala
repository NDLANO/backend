/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.testbase.UnitTestSuiteBase

class V42__AddIsMyNDLAFieldTest extends UnitTestSuiteBase {
  val migration = new V42__AddIsMyNDLAField

  test("That ismyndlafield is generated to true if owner is myndla user") {
    val ownerId  = "5fd02c6b-4b56-4824-a5a8-5e88cd5ba491"
    val before   = s"""{"owner":"$ownerId"}"""
    val expected = s"""{"owner":"$ownerId","isMyNDLAOwner":true}"""
    migration.convertColumn(before) should be(expected)
  }

  test("That ismyndlafield is generated to false if owner is auth0 user") {
    val ownerId  = "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT"
    val before   = s"""{"owner":"$ownerId"}"""
    val expected = s"""{"owner":"$ownerId","isMyNDLAOwner":false}"""
    migration.convertColumn(before) should be(expected)
  }
}
