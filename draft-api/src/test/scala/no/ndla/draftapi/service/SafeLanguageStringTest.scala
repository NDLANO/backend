/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.draftapi.model.domain.emptySomeToNone
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class SafeLanguageStringTest extends UnitSuite with TestEnvironment {

  test("emtpySomeToNone should return None on Some(\"\")") {
    emptySomeToNone(Some("")) should equal(None)
  }
  test("emtpySomeToNone should return Some with same content on non empty") {
    emptySomeToNone(Some("I have content :)")) should equal(Some("I have content :)"))
  }
  test("emtpySomeToNone should return None on None") {
    emptySomeToNone(None) should equal(None)
  }

}
