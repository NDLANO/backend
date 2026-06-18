/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.db.migration

import no.ndla.myndlaapi.{TestEnvironment, UnitSuite}

class V29__MigrateRobotConfigurationTest extends UnitSuite with TestEnvironment {
  val migration = new V29__MigrateRobotConfiguration

  test("That title is moved from root into settings") {
    val oldDocument =
      """{"title":"My Robot","version":"1.0","settings":{"name":"robot1","systemprompt":"Be helpful","question":"What do you need?","temperature":"0.7","model":"gpt-4"}}"""
    val expectedDocument =
      """{"version":"1.0","settings":{"name":"robot1","systemprompt":"Be helpful","question":"What do you need?","temperature":"0.7","model":"gpt-4","title":"My Robot","description":null,"voice":""}}"""
    migration.convertColumn(oldDocument) should be(expectedDocument)
  }

  test("That null systemprompt and question are converted to empty strings") {
    val oldDocument =
      """{"title":"My Robot","version":"1.0","settings":{"name":"robot1","systemprompt":null,"question":null,"temperature":"0.7","model":"gpt-4"}}"""
    val expectedDocument =
      """{"version":"1.0","settings":{"name":"robot1","systemprompt":"","question":"","temperature":"0.7","model":"gpt-4","title":"My Robot","description":null,"voice":""}}"""
    migration.convertColumn(oldDocument) should be(expectedDocument)
  }

  test("That description is preserved if already present") {
    val oldDocument =
      """{"title":"My Robot","version":"1.0","settings":{"name":"robot1","systemprompt":"Be helpful","question":"What do you need?","temperature":"0.7","model":"gpt-4","description":"A helpful robot"}}"""
    val expectedDocument =
      """{"version":"1.0","settings":{"name":"robot1","systemprompt":"Be helpful","question":"What do you need?","temperature":"0.7","model":"gpt-4","description":"A helpful robot","title":"My Robot","voice":""}}"""
    migration.convertColumn(oldDocument) should be(expectedDocument)
  }

  test("That voice is preserved if already present") {
    val oldDocument =
      """{"title":"My Robot","version":"1.0","settings":{"name":"robot1","systemprompt":"Be helpful","question":"What do you need?","temperature":"0.7","model":"gpt-4","voice":"nova"}}"""
    val expectedDocument =
      """{"version":"1.0","settings":{"name":"robot1","systemprompt":"Be helpful","question":"What do you need?","temperature":"0.7","model":"gpt-4","voice":"nova","title":"My Robot","description":null}}"""
    migration.convertColumn(oldDocument) should be(expectedDocument)
  }
}
