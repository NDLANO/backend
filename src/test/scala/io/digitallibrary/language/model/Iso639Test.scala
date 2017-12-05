/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.UnitSuite

class Iso639Test extends UnitSuite {
  test("that a valid iso 639-3 code is found for 2 letter input") {
    val id = Iso639.get("nb").map(_.id)
    id.isSuccess should be (true)
    id.get should equal ("nob")
  }

  test("that a valid iso 639-3 code is found for 3 letter input") {
    val id = Iso639.get("nob").map(_.id)
    id.isSuccess should be (true)
    id.get should equal ("nob")
  }

  test("that a 4-letter code returns a Failure") {
    Iso639.get("abcd").isFailure should be (true)
  }

  test("that an unknown language code returns a Failure") {
    Iso639.get("xyz").isFailure should be (true)
  }
}
