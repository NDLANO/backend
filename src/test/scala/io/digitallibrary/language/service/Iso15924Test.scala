/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.UnitSuite

class Iso15924Test extends UnitSuite {

  val iso15924 = new Iso15924

  test("that a valid code is found independent of case") {
    iso15924.findAlpha4("lAtN").map(_.code) should equal (Some("Latn"))
  }

  test("that an invalid code returns None") {
    iso15924.findAlpha4("abc") should be (None)
  }

  test("that empty string returns None") {
    iso15924.findAlpha4("") should be (None)
  }
}
