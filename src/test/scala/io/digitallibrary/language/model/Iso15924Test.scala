/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.UnitSuite

class Iso15924Test extends UnitSuite {

  test("that a valid code is found independent of case") {
    val code = Iso15924.get("lAtN").map(_.code)
    code.isSuccess should be (true)
    code.get should equal ("Latn")
  }

  test("that an invalid code returns a Failure") {
    Iso15924.get("abc").isFailure should be (true)
  }

  test("that empty string returns a Failure") {
    Iso15924.get("").isFailure should be (true)
  }
}
