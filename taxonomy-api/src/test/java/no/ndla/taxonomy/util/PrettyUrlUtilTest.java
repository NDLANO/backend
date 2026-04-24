/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import no.ndla.taxonomy.domain.NodeType;
import org.junit.jupiter.api.Test;

public class PrettyUrlUtilTest {

    @Test
    void test_create_pretty_url() {
        assertEquals(
                "/r/this-is-a-title/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "This is a title", "hash", NodeType.RESOURCE)
                        .get());
    }

    @Test
    void test_create_pretty_url_with_root() {
        assertEquals(
                "/e/the-root-title/this-is-a-title/hash",
                PrettyUrlUtil.createPrettyUrl(
                                Optional.of("The root title"), "This: is a 'title'", "hash", NodeType.TOPIC)
                        .get());
    }

    @Test
    void test_create_pretty_url_with_punctuation() {
        assertEquals(
                "/r/this-is-a-title-seriously/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "This is a title, seriously", "hash", NodeType.RESOURCE)
                        .get());
        assertEquals(
                "/r/this-is-a-title-and-a-12/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "This is a #title and a 1/2", "hash", NodeType.RESOURCE)
                        .get());
        assertEquals(
                "/r/this-is-a-title---with-long-dash/hash",
                PrettyUrlUtil.createPrettyUrl(
                                Optional.empty(), "This is a «title» – with \"long\" dash", "hash", NodeType.RESOURCE)
                        .get());
        assertEquals(
                "/r/pytagoras-setning/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "Pytagoras’ ”setning”", "hash", NodeType.RESOURCE)
                        .get());
        assertEquals(
                "/r/empezamos-ya/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "¡Empezamos ya!", "hash", NodeType.RESOURCE)
                        .get());
    }

    @Test
    void test_create_pretty_url_from_html_formatted_text() {
        assertEquals(
                "/f/this-is-a-italics-title/hash",
                PrettyUrlUtil.createPrettyUrl(
                                Optional.empty(), "This is a <em>italics title</em>", "hash", NodeType.SUBJECT)
                        .get());
    }

    @Test
    void test_create_pretty_url_with_weird_chars() {
        assertEquals(
                "/e/nar-kommer-hosten-tror-du-arlig-talt/hash",
                PrettyUrlUtil.createPrettyUrl(
                                Optional.empty(), "Når kommer høsten tror du ærlig talt?", "hash", NodeType.TOPIC)
                        .get());
        assertEquals(
                "/e/utgatt-historie/a-hoppe-etter-wirkola/hash",
                PrettyUrlUtil.createPrettyUrl(
                                Optional.of("Utgått historie"), "Å hoppe etter wirkola", "hash", NodeType.TOPIC)
                        .get());
        assertEquals(
                "/e/ektie-biejeme-tjaalegh/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "Ektie-bïejeme tjaalegh", "hash", NodeType.TOPIC)
                        .get());
        assertEquals(
                "/e/rebel-bihttos/hash",
                PrettyUrlUtil.createPrettyUrl(Optional.empty(), "Rebel (Bihttoš)", "hash", NodeType.TOPIC)
                        .get());
        assertEquals(
                "/e/suck-me-shakespeer-fack-ju-gohte/hash",
                PrettyUrlUtil.createPrettyUrl(
                                Optional.empty(), "Suck me Shakespeer (Fack ju Göhte)", "hash", NodeType.TOPIC)
                        .get());
    }

    @Test
    void test_get_hash_from_title() {
        assertEquals("hash", PrettyUrlUtil.getHashFromPath("/this-is-a-title/r/hash"));
        assertEquals("hash", PrettyUrlUtil.getHashFromPath("/this-is-a-title/e/hash"));
        assertEquals("hash", PrettyUrlUtil.getHashFromPath("/this-is-a-title/f/hash"));
        assertEquals("", PrettyUrlUtil.getHashFromPath("/this-is-a-title-without-hash"));
        assertEquals("", PrettyUrlUtil.getHashFromPath("/this-is-a-title-with-false-hash/n/hash"));

        // Must support old format
        assertEquals("hash", PrettyUrlUtil.getHashFromPath("/this-is-a-title__hash"));
    }
}
