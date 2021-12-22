/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import no.ndla.oembedproxy.UnitSuite

class OEmbedEndpointTest extends UnitSuite {

  val dummyEndpoint: OEmbedEndpoint = OEmbedEndpoint(None, None, None, None)

  test("That matches returns true for a matching expression") {
    dummyEndpoint.matches("http://www.ndla.no/*/test", "http://www.ndla.no/123123/test") should be(right = true)
    dummyEndpoint.matches("http://www.*.no/*/test/*", "http://www.aftenposten.no/123123/test/adf") should be(
      right = true)
    dummyEndpoint.matches("a.*.c", "a.b.c") should be(right = true)
  }

  test("That matches returns false for a non-matching expression") {
    dummyEndpoint.matches("http://www.ndla.no/*/test", "http://www.ndla.no/test") should be(right = false)
    dummyEndpoint.matches("http://www.ndla.no/*/test/*", "https://www.ndla.no/nb/test/123") should be(right = false)
  }

  test("That supports returns true if any of the scheme-patterns matches") {
    val schemes = List("http://www.ndla.no/*/test", "http://www.ndla.no/test/*")
    dummyEndpoint
      .copy(schemes = Some(schemes))
      .supports("http://www.ndla.no/123/test") should be(right = true)
    dummyEndpoint
      .copy(schemes = Some(schemes))
      .supports("http://www.ndla.no/test/123") should be(right = true)
  }

  test("That supports returns false when no of the scheme-patterns matches") {
    val schemes = List("http://www.ndla.no/*/test", "http://www.ndla.no/test/*")
    dummyEndpoint
      .copy(schemes = Some(schemes))
      .supports("http://www.vg.no") should be(right = false)
  }
}
