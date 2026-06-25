/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.ndla.taxonomy.domain.NodeType;
import org.junit.jupiter.api.Test;

public class PrettyUrlUtilTest {

    @Test
    void test_create_pretty_url() {
        assertEquals(
                "/r/this-is-a-title/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "This is a title", "hash", NodeType.RESOURCE));
    }

    @Test
    void test_create_pretty_url_with_root() {
        assertEquals(
                "/e/the-root-title/this-is-a-title/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl("The root title", "This: is a 'title'", "hash", NodeType.TOPIC));
    }

    @Test
    void test_create_pretty_url_with_punctuation() {
        assertEquals(
                "/r/this-is-a-title-seriously/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "This is a title, seriously", "hash", NodeType.RESOURCE));
        assertEquals(
                "/r/this-is-a-title-and-a-12/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "This is a #title and a 1/2", "hash", NodeType.RESOURCE));
        assertEquals(
                "/r/this-is-a-title---with-long-dash/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(
                        null, "This is a «title» – with \"long\" dash", "hash", NodeType.RESOURCE));
        assertEquals(
                "/r/pytagoras-setning/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "Pytagoras’ ”setning”", "hash", NodeType.RESOURCE));
        assertEquals(
                "/r/empezamos-ya/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "¡Empezamos ya!", "hash", NodeType.RESOURCE));
    }

    @Test
    void test_create_pretty_url_from_html_formatted_text() {
        assertEquals(
                "/f/this-is-a-italics-title/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(
                        null, "This is a <em>italics title</em>", "hash", NodeType.SUBJECT));
    }

    @Test
    void test_create_pretty_url_with_weird_chars() {
        assertEquals(
                "/e/nar-kommer-hosten-tror-du-arlig-talt/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(
                        null, "Når kommer høsten tror du ærlig talt?", "hash", NodeType.TOPIC));
        assertEquals(
                "/e/utgatt-historie/a-hoppe-etter-wirkola/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(
                        "Utgått historie", "Å hoppe etter wirkola", "hash", NodeType.TOPIC));
        assertEquals(
                "/e/ektie-biejeme-tjaalegh/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "Ektie-bïejeme tjaalegh", "hash", NodeType.TOPIC));
        assertEquals(
                "/e/rebel-bihttos/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(null, "Rebel (Bihttoš)", "hash", NodeType.TOPIC));
        assertEquals(
                "/e/suck-me-shakespeer-fack-ju-gohte/hash",
                PrettyUrlUtil.INSTANCE.createPrettyUrl(
                        null, "Suck me Shakespeer (Fack ju Göhte)", "hash", NodeType.TOPIC));
    }

    @Test
    void test_get_hash_from_title() {
        assertEquals("hash", PrettyUrlUtil.INSTANCE.getHashFromPath("/this-is-a-title/r/hash"));
        assertEquals("hash", PrettyUrlUtil.INSTANCE.getHashFromPath("/this-is-a-title/e/hash"));
        assertEquals("hash", PrettyUrlUtil.INSTANCE.getHashFromPath("/this-is-a-title/f/hash"));
        assertEquals("hash", PrettyUrlUtil.INSTANCE.getHashFromPath("/utdanning/this-is-a-title/hash"));
        assertEquals("", PrettyUrlUtil.INSTANCE.getHashFromPath("/this-is-a-title-without-hash"));
        assertEquals("", PrettyUrlUtil.INSTANCE.getHashFromPath("/this-is-a-title-with-false-hash/n/hash"));

        // Must support old format
        assertEquals("hash", PrettyUrlUtil.INSTANCE.getHashFromPath("/this-is-a-title__hash"));
    }
}
