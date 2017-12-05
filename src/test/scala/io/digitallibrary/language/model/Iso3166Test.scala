/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.UnitSuite

class Iso3166Test extends UnitSuite {

  test("that a valid code is found independent of case") {
    val code = Iso3166.get("no").map(_.code)
    code.isSuccess should be (true)
    code.get should equal ("NO")
  }

  test("that an invalid code returns a Failure") {
    Iso3166.get("abc").isFailure should be (true)
  }

  test("that empty string returns Failure") {
    Iso3166.get("").isFailure should be (true)
  }

}
