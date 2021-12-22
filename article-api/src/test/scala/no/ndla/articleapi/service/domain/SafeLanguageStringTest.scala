/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.domain

import no.ndla.articleapi.model.domain.emptySomeToNone
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

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
