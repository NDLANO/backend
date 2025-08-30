/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.common.Clock
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.searchapi.service.ConverterService
import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val converterService: ConverterService = new ConverterService
  override implicit lazy val clock: Clock                       = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers         = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling       = new ControllerErrorHandling
  override val controller: InternController                     = new InternController
  override implicit lazy val services: List[TapirController]    = List(controller)
  override implicit lazy val routes: Routes                     = new Routes
}
