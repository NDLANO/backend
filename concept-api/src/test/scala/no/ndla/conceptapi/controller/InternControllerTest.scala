/*
 * Part of NDLA concept-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi.controller

import no.ndla.conceptapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.Mockito.{reset, when}

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  val controller: InternController = new InternController

  override def beforeEach(): Unit = {
    reset(clock)
    when(clock.now()).thenCallRealMethod()
  }
}
