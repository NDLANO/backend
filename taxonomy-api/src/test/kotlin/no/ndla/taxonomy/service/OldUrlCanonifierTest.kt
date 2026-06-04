/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OldUrlCanonifierTest {
  private lateinit var canonifier: OldUrlCanonifier

  @BeforeEach
  fun init() {
    canonifier = OldUrlCanonifier()
  }

  @Test
  fun canonificationHandlesMenuSuffix() {
    val pathWithMenu = "ndla.no/nb/node/63920/menu"
    val result1 = canonifier.canonify(pathWithMenu)
    assertEquals("ndla.no/node/63920", result1)
    val pathWithNumberedMenu = "ndla.no/nb/node/13075/menu316?fag=56850"
    val result2 = canonifier.canonify(pathWithNumberedMenu)
    assertEquals("ndla.no/node/13075?fag=56850", result2)
    val pathWithMenuAndSlash = "ndla.no/nb/node/63920/menu/"
    val result3 = canonifier.canonify(pathWithMenuAndSlash)
    assertEquals("ndla.no/node/63920", result3)
  }

  @Test
  fun canonificationHandlesOembedSuffix() {
    val pathWithOembed = "ndla.no/nb/node/63920/oembed"
    val result1 = canonifier.canonify(pathWithOembed)
    assertEquals("ndla.no/node/63920", result1)
    val pathWithOembedAndParameter = "ndla.no/nb/node/13075/oembed?fag=56850"
    val result2 = canonifier.canonify(pathWithOembedAndParameter)
    assertEquals("ndla.no/node/13075?fag=56850", result2)
    val pathWithOembedAndSlash = "ndla.no/nb/node/63920/menu/"
    val result3 = canonifier.canonify(pathWithOembedAndSlash)
    assertEquals("ndla.no/node/63920", result3)
  }

  @Test
  fun canonificationHandlesDownloadSuffix() {
    val pathWithDownload = "ndla.no/nb/node/63920/download"
    val result1 = canonifier.canonify(pathWithDownload)
    assertEquals("ndla.no/node/63920", result1)
    val pathWithDownloadAndParameter = "ndla.no/nb/node/13075/download?fag=56850"
    val result2 = canonifier.canonify(pathWithDownloadAndParameter)
    assertEquals("ndla.no/node/13075?fag=56850", result2)
    val pathWithDownloadAndSlash = "ndla.no/nb/node/63920/download/"
    val result3 = canonifier.canonify(pathWithDownloadAndSlash)
    assertEquals("ndla.no/node/63920", result3)
  }

  @Test
  fun canonificationHandlesPrintPdfPrefix() {
    val pathWithPrintPdf = "ndla.no/nn/printpdf/50625"
    val result = canonifier.canonify(pathWithPrintPdf)
    assertEquals("ndla.no/node/50625", result)
  }

  @Test
  fun canonificationHandlesEasyReaderPrefix() {
    val pathWithPrintPdf = "ndla.no/nb/easyreader/8984"
    val result = canonifier.canonify(pathWithPrintPdf)
    assertEquals("ndla.no/node/8984", result)
  }

  @Test
  fun canonificationHandlesH5PEmbedPrefix() {
    val pathWithPrintPdf = "ndla.no/nb/h5p/embed/6124"
    val result = canonifier.canonify(pathWithPrintPdf)
    assertEquals("ndla.no/node/6124", result)
  }

  @Test
  fun canonificationHandlesH5PPrefix() {
    val pathWithPrintPdf = "ndla.no/nb/h5pcontent/132127"
    val result = canonifier.canonify(pathWithPrintPdf)
    assertEquals("ndla.no/node/132127", result)
  }

  @Test
  fun canonificationHandlesOtherParams() {
    val pathWithMenyParam = "ndla.no/nb/node/133111?fag=130693&meny=313944"
    val result = canonifier.canonify(pathWithMenyParam)
    assertEquals("ndla.no/node/133111?fag=130693", result)
  }

  @Test
  fun `handles urls ending with question mark`() {
    val pathWithEndingQuestionMark = "ndla.no/nb/node/133111?"
    val result = canonifier.canonify(pathWithEndingQuestionMark)
    assertEquals("ndla.no/node/133111", result)
  }
}
