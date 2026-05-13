/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.Clock
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.Mockito.{reset, when}

class ImageControllerV3Test extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  val controller: ImageControllerV3                          = new ImageControllerV3
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  override def beforeEach(): Unit = {
    reset(clock)
    when(clock.now()).thenCallRealMethod()
  }
}
