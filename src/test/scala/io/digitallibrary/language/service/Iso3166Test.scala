/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.UnitSuite

class Iso3166Test extends UnitSuite {

  val iso3166 = new Iso3166

  test("that a valid code is found independent of case") {
    iso3166.findAlpha2("no").map(_.code) should equal (Some("NO"))
  }

  test("that an invalid code returns None") {
    iso3166.findAlpha2("abc") should be (None)
  }

  test("that empty string returns None") {
    iso3166.findAlpha2("") should be (None)
  }

}
