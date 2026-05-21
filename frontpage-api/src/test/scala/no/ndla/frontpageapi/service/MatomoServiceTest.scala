/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}

class MatomoServiceTest extends UnitSuite with TestEnvironment {
  override implicit lazy val matomoService: MatomoService = new MatomoService

  test("taxonomy context id should be extracted from url") {
    val ctxIds = List("f009643d3a", "223342990757")
    ctxIds.foreach { ctxId =>
      val toExtract = List(
        s"https://ndla.no/r/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId/6924",
        s"https://ndla.no/nn/r/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId/6924",
        s"ndla.no/nn/r/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId/6924",
        s"https://ndla.no/r/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId",
        s"https://ndla.no/nb/r/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId",
        s"https://ndla.no/nb/e/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId",
        s"https://ndla.no/sma/e/verktoykassa---for-larere/planlegg-en-laringssti/$ctxId",
        s"https://ndla.no/nb/e/$ctxId",
      )

      val toFail = List(
        s"https://ndla.no/nb/utdanning/bygg--og-anleggsteknikk/$ctxId",
        s"https://ndla.no/utdanning/bygg--og-anleggsteknikk/$ctxId",
        s"https://ndla.no/f/praktisk-yrkesutovelse-ba-bat-vg1/$ctxId",
        s"https://ndla.no/nn/f/praktisk-yrkesutovelse-ba-bat-vg1/$ctxId",
      )

      toExtract.foreach { url =>
        val res = matomoService.extractContextId(url)
        if (!res.contains(ctxId)) fail(s"Failed to extract context id from url: $url, got: $res\n")
      }

      toFail.foreach { url =>
        val res = matomoService.extractContextId(url)
        if (res.nonEmpty) fail(s"Should not have extracted context id from url: $url, got: $res\n")
      }
    }
  }

}
