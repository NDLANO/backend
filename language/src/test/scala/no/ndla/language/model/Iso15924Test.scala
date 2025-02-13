/*
 * Part of NDLA language
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

import no.ndla.language.UnitSuite

class Iso15924Test extends UnitSuite {

  test("that a valid code is found independent of case") {
    val code = Iso15924.get("lAtN").map(_.code)
    code.isSuccess should be(true)
    code.get should equal("Latn")
  }

  test("that an invalid code returns a Failure") {
    Iso15924.get("abc").isFailure should be(true)
  }

  test("that empty string returns a Failure") {
    Iso15924.get("").isFailure should be(true)
  }
}
