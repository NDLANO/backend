/*
 * Part of NDLA language
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

import no.ndla.language.UnitSuite

class Iso3166Test extends UnitSuite {

  test("that a valid code is found independent of case") {
    val code = Iso3166.get("no").map(_.code)
    code.isSuccess should be(true)
    code.get should equal("NO")
  }

  test("that an invalid code returns a Failure") {
    Iso3166.get("abc").isFailure should be(true)
  }

  test("that empty string returns Failure") {
    Iso3166.get("").isFailure should be(true)
  }

}
