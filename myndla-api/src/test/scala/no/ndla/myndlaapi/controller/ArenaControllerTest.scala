/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.api.config.{ConfigMeta, ConfigMetaValue}
import no.ndla.myndla.model.domain.config.ConfigKey
import no.ndla.myndlaapi.TestData.{adminAndWriteScopeClientToken, adminScopeClientToken}
import no.ndla.myndlaapi.{Eff, TestEnvironment}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.scalatestsuite.UnitTestSuite
import sttp.client3.quick._

import scala.util.Success

class ArenaControllerTest extends UnitTestSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  val controller                            = new ArenaController()
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  test("That token parsing works") { ??? }
  test("That admin token parsing works") { ??? }
  test("That recent endpoint works along with id endpoint for topics") { ??? }
}
