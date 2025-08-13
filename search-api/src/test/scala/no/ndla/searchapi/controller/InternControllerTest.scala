/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override lazy val converterService        = new ConverterService
  override val controller: InternController = new InternController
}
