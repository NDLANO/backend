/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OldUrlCanonifierTest {

    private OldUrlCanonifier canonifier;

    @BeforeEach
    public void init() {
        canonifier = new OldUrlCanonifier();
    }

    @Test
    public void canonificationHandlesMenuSuffix() {
        String pathWithMenu = "ndla.no/nb/node/63920/menu";
        String result1 = canonifier.canonify(pathWithMenu);
        assertEquals("ndla.no/node/63920", result1);
        String pathWithNumberedMenu = "ndla.no/nb/node/13075/menu316?fag=56850";
        String result2 = canonifier.canonify(pathWithNumberedMenu);
        assertEquals("ndla.no/node/13075?fag=56850", result2);
        String pathWithMenuAndSlash = "ndla.no/nb/node/63920/menu/";
        String result3 = canonifier.canonify(pathWithMenuAndSlash);
        assertEquals("ndla.no/node/63920", result3);
    }

    @Test
    public void canonificationHandlesOembedSuffix() {
        String pathWithOembed = "ndla.no/nb/node/63920/oembed";
        String result1 = canonifier.canonify(pathWithOembed);
        assertEquals("ndla.no/node/63920", result1);
        String pathWithOembedAndParameter = "ndla.no/nb/node/13075/oembed?fag=56850";
        String result2 = canonifier.canonify(pathWithOembedAndParameter);
        assertEquals("ndla.no/node/13075?fag=56850", result2);
        String pathWithOembedAndSlash = "ndla.no/nb/node/63920/menu/";
        String result3 = canonifier.canonify(pathWithOembedAndSlash);
        assertEquals("ndla.no/node/63920", result3);
    }

    @Test
    public void canonificationHandlesDownloadSuffix() {
        String pathWithDownload = "ndla.no/nb/node/63920/download";
        String result1 = canonifier.canonify(pathWithDownload);
        assertEquals("ndla.no/node/63920", result1);
        String pathWithDownloadAndParameter = "ndla.no/nb/node/13075/download?fag=56850";
        String result2 = canonifier.canonify(pathWithDownloadAndParameter);
        assertEquals("ndla.no/node/13075?fag=56850", result2);
        String pathWithDownloadAndSlash = "ndla.no/nb/node/63920/download/";
        String result3 = canonifier.canonify(pathWithDownloadAndSlash);
        assertEquals("ndla.no/node/63920", result3);
    }

    @Test
    public void canonificationHandlesPrintPdfPrefix() {
        String pathWithPrintPdf = "ndla.no/nn/printpdf/50625";
        String canonifiedResult = canonifier.canonify(pathWithPrintPdf);
        assertEquals("ndla.no/node/50625", canonifiedResult);
    }

    @Test
    public void canonificationHandlesEasyReaderPrefix() {
        String pathWithPrintPdf = "ndla.no/nb/easyreader/8984";
        String canonifiedResult = canonifier.canonify(pathWithPrintPdf);
        assertEquals("ndla.no/node/8984", canonifiedResult);
    }

    @Test
    public void canonificationHandlesH5PEmbedPrefix() {
        String pathWithPrintPdf = "ndla.no/nb/h5p/embed/6124";
        String canonifiedResult = canonifier.canonify(pathWithPrintPdf);
        assertEquals("ndla.no/node/6124", canonifiedResult);
    }

    @Test
    public void canonificationHandlesH5PPrefix() {
        String pathWithPrintPdf = "ndla.no/nb/h5pcontent/132127";
        String canonifiedResult = canonifier.canonify(pathWithPrintPdf);
        assertEquals("ndla.no/node/132127", canonifiedResult);
    }

    @Test
    public void canonificationHandlesOtherParams() {
        String pathWithMenyParam = "ndla.no/nb/node/133111?fag=130693&meny=313944";
        String canonifiedResult = canonifier.canonify(pathWithMenyParam);
        assertEquals("ndla.no/node/133111?fag=130693", canonifiedResult);
    }
}
