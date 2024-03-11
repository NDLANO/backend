/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.service

import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.myndla.{TestData, TestEnvironment}
import no.ndla.myndla.model.api.config.ConfigMetaValue
import no.ndla.myndla.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta}
import no.ndla.network.tapir.auth.Permission.{LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_PUBLISH}
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.scalatestsuite.UnitTestSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class ConfigServiceTest extends UnitTestSuite with TestEnvironment {

  val service: ConfigService = new ConfigService

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
  }

  val testConfigMeta: ConfigMeta = ConfigMeta(
    ConfigKey.LearningpathWriteRestricted,
    value = BooleanValue(true),
    TestData.today,
    "EnKulFyr"
  )

  test("That updating config returns failure for non-admin users") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(testConfigMeta))
    val Failure(ex) = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(true),
      TokenUser("Kari", Set(LEARNINGPATH_API_PUBLISH), None)
    )
    ex.isInstanceOf[AccessDeniedException] should be(true)
  }

  test("That updating config returns success if all is good") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(testConfigMeta))
    val Success(_) = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(true),
      TokenUser("Kari", Set(LEARNINGPATH_API_ADMIN), None)
    )
  }

  test("That validation fails if IsWriteRestricted is not a boolean") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(testConfigMeta))
    val Failure(ex) = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(List("123")),
      TokenUser("Kari", Set(LEARNINGPATH_API_ADMIN), None)
    )

    ex.isInstanceOf[ValidationException] should be(true)
  }

  test("That validation succeeds if IsWriteRestricted is a boolean") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(testConfigMeta))
    val res = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(true),
      TokenUser("Kari", Set(LEARNINGPATH_API_ADMIN), None)
    )
    res.isSuccess should be(true)
  }

}
