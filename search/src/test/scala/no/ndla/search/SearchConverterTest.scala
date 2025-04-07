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

  test("That extracting imageids works") {
    val imageId = "123"
    val html =
      s"""<section><h1>Hello my dear friends</h1><ndlaembed data-resource="image" data-resource_id="$imageId"></ndlaembed>"""

    val expected = List(
      EmbedValues(
        id = List(imageId),
        resource = Some("image"),
        language = "nb"
      )
    )

    SearchConverter.getEmbedValues(html, "nb") should be(expected)

  }

  test("That extracting videoids from html strips timestamps") {
    val videoId        = "2398472394"
    val videoIdAndData = s"$videoId&amp;t="
    val html =
      s"""<section><h1>Hello my dear friends</h1><ndlaembed data-resource="video" data-videoid="$videoIdAndData"></ndlaembed>"""

    val expected = List(
      EmbedValues(
        id = List(videoId),
        resource = Some("video"),
        language = "nb"
      )
    )

    SearchConverter.getEmbedValues(html, "nb") should be(expected)
  }
}
