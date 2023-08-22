/*
 * Part of NDLA article-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import cats.effect.IO
import no.ndla.scalatestsuite.UnitTestSuite

class SomeIOTest extends UnitTestSuite {
  test("yolo") {
    IO(1).map(x => {
      x should be(1)
    })
  }

  test("yolo2") {
    1 should be(1)
    1 should be(2)
  }
}
