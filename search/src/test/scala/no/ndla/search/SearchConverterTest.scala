/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import no.ndla.search.model.domain.EmbedValues
import no.ndla.testbase.UnitTestSuiteBase

class SearchConverterTest extends UnitTestSuiteBase {

  test("That extracting imageids and videoids works") {
    {
      val imageId = "123"
      val html    =
        s"""<section><h1>Hello my dear friends</h1><ndlaembed data-resource="image" data-resource_id="$imageId"></ndlaembed>"""

      val expected = List(EmbedValues(id = List(imageId), resource = Some("image"), language = "nb"))

      SearchConverter.getEmbedValues(html, "nb") should be(expected)
    }
    {
      val videoId = "5796284585001"
      val html    =
        s"""<section><h1>Hello my dear friends</h1><ndlaembed data-account="4806596774001" data-caption="Kildebruk" data-player="BkLm8fT" data-resource="brightcove" data-videoid="$videoId"></ndlaembed>"""

      val expected = List(EmbedValues(id = List(videoId), resource = Some("brightcove"), language = "nb"))
      SearchConverter.getEmbedValues(html, "nb") should be(expected)
    }
  }

  test("That extracting videoids from html strips timestamps") {
    val videoId        = "2398472394"
    val videoIdAndData = s"$videoId&amp;t="
    val html           =
      s"""<section><h1>Hello my dear friends</h1><ndlaembed data-resource="video" data-videoid="$videoIdAndData"></ndlaembed>"""

    val expected = List(EmbedValues(id = List(videoId), resource = Some("video"), language = "nb"))

    SearchConverter.getEmbedValues(html, "nb") should be(expected)
  }

  test("That extracting ids from urls works") {
    {
      val videoId        = "1234"
      val videoIdAndData = s"https://ndla2.filmiundervisning.no/film/$videoId"
      val html           =
        s"""<section><h1>Hello my dear friends</h1><ndlaembed data-resource="iframe" data-url="$videoIdAndData"></ndlaembed>"""

      val expected = List(EmbedValues(id = List(videoIdAndData, videoId), resource = Some("iframe"), language = "nb"))
      SearchConverter.getEmbedValues(html, "nb") should be(expected)
    }
    {
      val videoId  = "MSUI20000011"
      val videoUrl = s"https://static.nrk.no/ludo/latest/video-embed.html#id=$videoId"
      val html     =
        s"""<section><h1>Hello my dear friends</h1><ndlaembed data-resource="iframe" data-url="$videoUrl" data-type="iframe"></ndlaembed>"""

      val expected = List(EmbedValues(id = List(videoUrl, videoId), resource = Some("iframe"), language = "nb"))
      SearchConverter.getEmbedValues(html, "nb") should be(expected)
    }
  }

}
