/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.config.{ConfigMetaDTO, ConfigMetaValueDTO}
import no.ndla.common.model.domain.config.ConfigKey
import no.ndla.myndlaapi.TestData.{adminAndWriteScopeClientToken, adminScopeClientToken}
import no.ndla.myndlaapi.TestEnvironment
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import sttp.client3.quick.*

import scala.util.Success

class ConfigControllerTest extends UnitTestSuite with TestEnvironment with TapirControllerTest {
  val controller: ConfigController = new ConfigController()

  test("That updating config returns 200 if all is good") {
    when(configService.updateConfig(any[ConfigKey], any[ConfigMetaValueDTO], any[TokenUser]))
      .thenReturn(
        Success(
          ConfigMetaDTO(
            ConfigKey.LearningpathWriteRestricted.entryName,
            Left(true),
            NDLADate.now(),
            "someoneCool"
          )
        )
      )

    val response1 = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/myndla-api/v1/config/${ConfigKey.LearningpathWriteRestricted.entryName}")
        .body("{\"value\": true}")
        .header("Authorization", s"Bearer $adminScopeClientToken")
    )
    response1.code.code should be(200)

    val response2 = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/myndla-api/v1/config/${ConfigKey.LearningpathWriteRestricted.entryName}")
        .body("{\"value\": true}")
        .header("Authorization", s"Bearer $adminAndWriteScopeClientToken")
    )
    response2.code.code should be(200)
  }
}
