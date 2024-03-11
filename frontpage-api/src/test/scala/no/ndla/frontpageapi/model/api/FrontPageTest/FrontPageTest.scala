/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api.FrontPageTest

import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import no.ndla.frontpageapi.model.api.{FrontPage, Menu}

class FrontPageTest extends UnitSuite with TestEnvironment {
  test("test that circe encoding and decoding works for recursive types") {
    val before =
      FrontPage(1, List(Menu(2, List(Menu(3, List(Menu(4, List(), Some(false))), Some(false))), Some(false))))
    val jsonString = before.asJson.noSpaces
    val parsed     = parse(jsonString).toTry.get
    val converted  = parsed.as[FrontPage].toTry.get
    converted should be(before)
  }
}
