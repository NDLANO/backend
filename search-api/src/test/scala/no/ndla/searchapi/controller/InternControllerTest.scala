/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  override val converterService    = new ConverterService
  val controller: InternController = new InternController
}
