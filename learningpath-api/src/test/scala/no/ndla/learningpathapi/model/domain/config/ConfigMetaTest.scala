/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import no.ndla.learningpathapi.{TestData, UnitSuite, UnitTestEnvironment}
import no.ndla.myndla.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta}

class ConfigMetaTest extends UnitSuite with UnitTestEnvironment {

  test("That validation exists for all configuration parameters") {
    ConfigKey.values.foreach(key => {
      try {
        ConfigMeta(
          key = key,
          value = BooleanValue(true),
          updatedAt = TestData.today,
          updatedBy = "OneCoolKid"
        ).validate
      } catch {
        case _: Throwable =>
          fail(
            s"Every ConfigKey value needs to be validated. '${key.entryName}' threw an exception when attempted validation."
          )
      }
    })
  }

}
