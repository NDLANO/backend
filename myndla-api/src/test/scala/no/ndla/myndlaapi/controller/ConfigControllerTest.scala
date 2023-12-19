/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.api.config.{ConfigMeta, ConfigMetaValue}
import no.ndla.myndla.model.domain.config.ConfigKey
import no.ndla.myndlaapi.TestData.{adminAndWriteScopeClientToken, adminScopeClientToken}
import no.ndla.myndlaapi.{Eff, TestEnvironment}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.scalatestsuite.UnitTestSuite
import sttp.client3.quick._

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class ConfigControllerTest extends UnitTestSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  val controller                            = new ConfigController()
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    Future { Routes.startJdkServer(this.getClass.getName, serverPort) {} }
    Thread.sleep(1000)
  }

  test("That updating config returns 200 if all is good") {
    Thread.sleep(10000)
    when(configService.updateConfig(any[ConfigKey], any[ConfigMetaValue], any[TokenUser]))
      .thenReturn(
        Success(
          ConfigMeta(
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
