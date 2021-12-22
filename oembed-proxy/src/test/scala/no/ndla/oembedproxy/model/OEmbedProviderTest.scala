/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import no.ndla.oembedproxy.UnitSuite

class OEmbedProviderTest extends UnitSuite {

  val youtubeProvider: OEmbedProvider =
    OEmbedProvider("youtube", "https://www.youtube.com", List())

  val ndlaEndpoint: OEmbedEndpoint =
    OEmbedEndpoint(Some(List("https://ndla.no/*/123")), Some("https://ndla.no/oembed"), None, None)

  val ndlaListEndpoint: OEmbedEndpoint =
    OEmbedEndpoint(Some(List("https://liste.ndla.no/*")), Some("https://liste.ndla.no/oembed"), None, None)

  val youtubeEndpoint: OEmbedEndpoint =
    OEmbedEndpoint(Some(List("https://www.youtube.com/*")), Some("https://www.youtube.com/oembed"), None, None)

  test("That hostMatches returns true for same host, regardless of protocol") {
    youtubeProvider.hostMatches("https://www.youtube.com") should be(right = true)
  }

  test("That hostMatches returns false for different hosts") {
    youtubeProvider.hostMatches("https://www.ted.com") should be(right = false)
  }

  test("That hostMatches returns false for nonexistant hosts") {
    youtubeProvider.copy(providerUrl = "https:///onlypathere").hostMatches("https:///onlypathere") should be(
      right = false)
  }

  test("That supports returns true when host matches") {
    youtubeProvider.supports("https://www.youtube.com") should be(right = true)
  }

  test("That supports returns true when endpoints matches") {
    val provider = youtubeProvider.copy(endpoints = List(ndlaEndpoint, ndlaListEndpoint))
    provider.supports("https://ndla.no/nb/123") should be(right = true)
    provider.supports("https://liste.ndla.no/nb/123") should be(right = true)
  }

  test("That support returns false when neither endpoints or host matches") {
    youtubeProvider
      .copy(endpoints = List(youtubeEndpoint))
      .supports("https://www.ndla.no/nb/123") should be(right = false)
  }

  test("That requestUrl throws exception when no endpoints have embedUrl defined") {
    assertResult("The provider 'youtube' has no embed-url available") {
      intercept[RuntimeException] {
        youtubeProvider.requestUrl("random", None, None)
      }.getMessage
    }
  }

  test("That {format} is replaced in embedUrl") {
    val endpoint =
      youtubeEndpoint.copy(url = Some("https://www.youtube.com/oembed.{format}"))
    val requestUrl = youtubeProvider
      .copy(endpoints = List(endpoint))
      .requestUrl("https://www.youtube.com/v/ABC", None, None)
    requestUrl should equal("https://www.youtube.com/oembed.json?url=https://www.youtube.com/v/ABC&format=json")
  }

  test("That maxwidth is appended correctly") {
    val endpoint = youtubeEndpoint.copy(url = Some("https://youtube.com/oembed"))
    val requestUrl = youtubeProvider
      .copy(endpoints = List(endpoint))
      .requestUrl("https://www.youtube.com/v/ABC", Some("100"), None)
    requestUrl should equal("https://youtube.com/oembed?url=https://www.youtube.com/v/ABC&format=json&maxwidth=100")
  }

  test("That maxheight is appended correctly") {
    val endpoint = youtubeEndpoint.copy(url = Some("https://youtube.com/oembed"))
    val requestUrl = youtubeProvider
      .copy(endpoints = List(endpoint))
      .requestUrl("https://www.youtube.com/v/ABC", None, Some("100"))
    requestUrl should equal("https://youtube.com/oembed?url=https://www.youtube.com/v/ABC&format=json&maxheight=100")
  }

  test("That both maxwidth and maxheight are appended correctly") {
    val endpoint = youtubeEndpoint.copy(url = Some("https://youtube.com/oembed"))
    val requestUrl = youtubeProvider
      .copy(endpoints = List(endpoint))
      .requestUrl("https://www.youtube.com/v/ABC", Some("100"), Some("200"))
    requestUrl should equal(
      "https://youtube.com/oembed?url=https://www.youtube.com/v/ABC&format=json&maxwidth=100&maxheight=200")
  }
}
