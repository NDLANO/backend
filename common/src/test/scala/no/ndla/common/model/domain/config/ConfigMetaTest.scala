/*
 * Part of NDLA common
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.config

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta}
import no.ndla.testbase.UnitTestSuiteBase

class ConfigMetaTest extends UnitTestSuiteBase {

  test("That validation exists for all configuration parameters") {
    ConfigKey
      .values
      .foreach(key => {
        try {
          ConfigMeta(key = key, value = BooleanValue(true), updatedAt = NDLADate.now(), updatedBy = "OneCoolKid")
            .validate
        } catch {
          case _: Throwable => fail(
              s"Every ConfigKey value needs to be validated. '${key.entryName}' threw an exception when attempted validation."
            )
        }
      })
  }

}
