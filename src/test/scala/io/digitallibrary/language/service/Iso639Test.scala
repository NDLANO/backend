/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.UnitSuite

class Iso639Test extends UnitSuite {

  val iso639 = new Iso639()

  test("that a valid iso 639-3 code is found") {
    iso639.findAlpha3("nob").map(_.id) should equal (Some("nob"))
  }

  test("that a valid iso 639-1 code is found and 639-3 returned") {
    iso639.findAlpha3("nb").map(_.id) should equal (Some("nob"))
  }

  test("that a 4-letter code returns None") {
    iso639.findAlpha3("abcd") should be (None)
  }

  test("that an unknown language code returns None") {
    iso639.findAlpha3("b1b") should be (None)
  }

}
